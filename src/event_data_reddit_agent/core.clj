(ns event-data-reddit-agent.core
  (:require [org.crossref.event-data-agent-framework.core :as c]
            [org.crossref.event-data-agent-framework.util :as agent-util]
            [org.crossref.event-data-agent-framework.web :as agent-web]
            [crossref.util.doi :as cr-doi])
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :refer [reader]]
            [clojure.data.json :as json])
  (:require [clj-time.coerce :as coerce]
            [clj-time.core :as clj-time]
            [throttler.core :refer [throttle-fn]]
            [org.httpkit.client :as http]
            [config.core :refer [env]]
            [korma.core :as k]
            [korma.db :as kdb])
  (:import [java.util UUID])
   (:gen-class))

(def source-token "a6c9d511-9239-4de8-a266-b013f5bd8764")
(def version "0.1.0")
(def user-agent "CrossrefEventData:eventdata.crossref.org (by /u/crossref-bot labs@crossref.org)")

(kdb/defdb db (kdb/mysql {:db (:db-name env)
                          :host (:db-host env) 
                          :port (Integer/parseInt (:db-port env))
                          :user (:db-user env)
                          :password (:db-password env)}))


; The date we saw a URL in a reddit item.
; A reddit item may have more than one URL!
(k/defentity seen-reddit-item
  (k/table "seen_reddit_item")
  (k/pk :id)
  (k/entity-fields :id :reddit_id :url :seen)
  (k/transform (fn [{seen :seen :as obj}]
                 (if-not seen obj
                   (assoc obj :seen (str (coerce/from-sql-date seen)))))))

(defn seen?
  "Return date we saw the url in the item or nil."
  [url reddit-id]
  (->
    (k/select seen-reddit-item (k/where {:reddit_id reddit-id :url url}))
    first 
    :seen))

(defn seen!
  [url reddit-id]
  (c/send-heartbeat "newsfeed-agent/process/mark-seen" 1)
  (k/exec-raw ["INSERT IGNORE INTO seen_reddit_item (url, reddit_id, seen) VALUES (?,?,?)" [url reddit-id (coerce/to-sql-time (clj-time/now))]]))

; Auth
(def reddit-token (atom nil))
(defn fetch-reddit-token
  "Fetch a new Reddit token."
  []
  (c/send-heartbeat "newsfeed-agent/reddit/authenticate" 1)
  (let [response @(http/post
                    "https://www.reddit.com/api/v1/access_token"
                     {:as :text
                      :headers {"User-Agent" user-agent}
                      :form-params {"grant_type" "password"
                                    "username" (:reddit-app-name env)
                                    "password" (:reddit-password env)}
                      :basic-auth [(:reddit-client env) (:reddit-secret env)]})
        token (when-let [body (:body response)]
                (->
                  (json/read-str body :key-fn keyword)
                  :access_token))]
    token))

(defn check-reddit-token
  "Check the current Reddit token. Return if it works."
  []
  (let [token @reddit-token]
    (-> "https://oauth.reddit.com/api/v1/me"
        (http/get {:headers {"User-Agent" user-agent
                             "Authorization" (str "bearer " token)}})
        deref
        :status
        (when (= 200)
          token))))
      
(defn get-reddit-token
  "Return valid token. Fetch new one if necessary."
  []
  (if-let [valid-token (check-reddit-token)]
    valid-token
    (do
      (reset! reddit-token (fetch-reddit-token))
      (check-reddit-token))))

; API
(defn parse-page
  "Parse response JSON. Return
  {:after-token ; can be nil
   :items [{:url, :title, :permalink :created_utc :subreddit :kind :id}]}"
  [json-data]
  (let [parsed (json/read-str json-data)
        after-token (get-in parsed ["data" "after"])
        childs (get-in parsed ["data" "children"])]
    {:after-token after-token
     :items (map (fn [child]
                  { :url (get-in child ["data" "url"])
                    :id (get-in child ["data" "id"])
                    :title (get-in child ["data" "title"])
                    :permalink (get-in child ["data" "permalink"])
                    :created_utc (long (get-in child ["data" "created_utc"]))
                    :subreddit (get-in child ["data" "subreddit"])
                    :kind (get child "kind")}) childs)}))

(defn fetch-page
  "Fetch the API result, return the URL used and the data"
  [domain after-token]
  (c/send-heartbeat "newsfeed-agent/reddit/fetch-page" 1)
  (let [url (str "https://oauth.reddit.com/domain/" domain "/new.json?sort=new&after=" after-token)
        _ (log/info "Fetch" url)
        result @(http/get url {:headers {"User-Agent" user-agent
                                         "Authorization" (str "bearer " (get-reddit-token))}})]
    (log/info "Fetched" url)
    [url (parse-page (:body result))]))

(def fetch-page-throttled (throttle-fn fetch-page 30 :minute))

(defn fetch-pages
  "Lazy sequence of pages for the domain."
  ([domain]
    (fetch-pages domain nil))
  
  ([domain after-token]
    (let [result (fetch-page-throttled domain after-token)
          ; Token for next page. If this is null then we've reached the end of the iteration.
          next-token (-> result second :after-token)]
      
      (if next-token
        (lazy-seq (cons result (fetch-pages domain next-token)))
        [result]))))

(defn fetch-parsed-pages
  "Fetch all API for a domain that we haven't seen yet, plus process enough to know when we saw them and which DOIs have been found.
  Return seq of [url page-data decorated-items]"
  [domain]
  (let [; Pages as seq of [url result] pairs.
        pages (fetch-pages domain)
                        
        ; Map pages from [url page] into [url page decorated-items]
        ; In decorated-items, associate each item in the page with date the URL was seen before and the DOI match attempt.
        decorate-seen (map (fn [[url page]]
                             (let [items (:items page)
                                   decorated-items (map
                                                     (fn [item]
                                                       (assoc item :seen-before-date (seen? (:url item) (:id item))
                                                                   ; as {:doi :version :query}
                                                                   :url-doi-match (agent-web/query-reverse-api (:url item))))
                                                     items)]
                                [url page decorated-items])) pages)
        
        ; Keep going until we've find a page where we've seen it all.
        unseen-pages (take-while (fn [[_ _ decorated-items]]
                                   (every? #(-> % :seen-before-date nil?) decorated-items)) decorate-seen)
        
        ; Take one extra, just to show a page of justification about why we stopped.
        unseen-plus-one (take (inc (count unseen-pages)) decorate-seen)]
  unseen-plus-one))

; Evidence building

; https://www.reddit.com/dev/api/
(def work-types
  "Mapping of reddit object types to lagotto work types. Only expect ever to see t1 and t3.
  In any case, events only get this far if they have a URL that matched a DOI."

{"t1" "personal_communication" ; comment
 "t2" "webpage" ; Account
 "t3" "post" ; Link
 "t4" "personal_communication"; Message
 "t5" "webpage" ; Subreddit
 "t6" "webpage" ; Award
 "t8" "webapge" ; PromoCampaign
})

(defn build-deposit
  "Process an item from an Reddit response and return a deposit."
  [item]
  (let [id (:id item)
        timestamp (str (coerce/from-long
                        (* 1000 (:created_utc item))))

        title (:title item)
        author-name (when-let [author-name (:author item)]
                      (str "https://reddit.com/u/" author-name))

        url (str "https://reddit.com" (:permalink item))
        work-type (work-types (:kind item))]
    
    {
      "obj_id" (cr-doi/normalise-doi (-> item :url-doi-match :doi))
      "source_token" source-token
      "occurred_at" timestamp
      "subj_id" url,
      "action" "added",
      "subj" {
        "title" title,
        "issued" timestamp,
        "pid" url,
        "URL" url,
        "type" work-type
      },
      "uuid" (str (UUID/randomUUID)),
      "source_id" "reddit",
      "relation_type_id" "discusses"}))
  
(defn evidence-for-domain
  [domain-artifact-url domain]
  (let [pages (fetch-parsed-pages domain)
        evidence-input (into {} (map (fn [[url page-input _]]
                                  [url page-input]) pages))
        
        ; All input items from all pages.
        input-items (apply concat (map (fn [[_ _ decorated-items]] decorated-items) pages))
        
        ; All items we've not seen that had a match.
        interested-items (->> input-items
                             (remove :seen-before-date)
                             (filter #(-> % :url-doi-match :doi)))
                          
        deposits (map (partial build-deposit) interested-items)]
    
    {:agent {:name "reddit" :version version}
     :run (str (clj-time/now))
     :artifacts [domain-artifact-url]
     :input evidence-input
     :processing {:items input-items
                  :interested-items interested-items}
     :deposits deposits}))

(defn check-all-domains
  "Check all domains for unseen links."
  [artifacts send-evidence-callback]
  (log/info "Start crawl all Domains on Reddit at" (str (clj-time/now)))
  (c/send-heartbeat "newsfeed-agent/process/scan-domains" 1)
  (let [[domain-list-url domain-list-f] (get artifacts "domain-list")]
    (with-open [domain-reader (reader domain-list-f)]
      (let [domains (line-seq domain-reader)
            evidence-records (map (partial evidence-for-domain domain-list-url) domains)]
        (doseq [er evidence-records]
          (send-evidence-callback er)
          (c/send-heartbeat "newsfeed-agent/process/found-doi" (count (:deposits er)))
          (doseq [item (-> er :processing :interested-items)]
            (seen! (:url item) (:id item)))))))
  (c/send-heartbeat "newsfeed-agent/process/finish-scan-domains" 1))

(def agent-definition
  {:agent-name "reddit-agent"
   :version version
   :schedule [{:name "check-all-domains"
              :seconds 1800 ; wait half an hour between scans
              :fixed-delay true
              :fun check-all-domains
              :required-artifacts ["domain-list"]}]
   :runners []})

(defn -main [& args]
  (c/run args agent-definition))

