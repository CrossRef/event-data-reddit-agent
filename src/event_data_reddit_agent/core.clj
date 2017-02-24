(ns event-data-reddit-agent.core
  (:require [org.crossref.event-data-agent-framework.core :as c]
            [event-data-common.status :as status]
            [crossref.util.doi :as cr-doi]
            [clojure.tools.logging :as log]
            [clojure.java.io :refer [reader]]
            [clojure.data.json :as json]
            [clj-time.coerce :as coerce]
            [clj-time.core :as clj-time]
            [throttler.core :refer [throttle-fn]]
            [org.httpkit.client :as http]
            [config.core :refer [env]]
            [clojure.core.async :refer [>!!]]
            [robert.bruce :refer [try-try-again]])
  (:import [java.util UUID]
           [org.apache.commons.codec.digest DigestUtils])
  (:gen-class))

(def source-token "a6c9d511-9239-4de8-a266-b013f5bd8764")
(def version "0.1.6")
(def user-agent "CrossrefEventDataBot (eventdata@crossref.org) (by /u/crossref-bot labs@crossref.org)")

; Auth
(def reddit-token (atom nil))
(defn fetch-reddit-token
  "Fetch a new Reddit token."
  []
  (status/send! "reddit-agent" "reddit" "authenticate" 1)
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
  (let [token @reddit-token
        result (-> "https://oauth.reddit.com/api/v1/me"
                 (http/get {:headers {"User-Agent" user-agent
                            "Authorization" (str "bearer " token)}})
        deref)]
      (if-not (= (:status result) 200)
        (log/error "Couldn't verify OAuth Token" token " got " result)
        token)))

(defn get-reddit-token
  "Return valid token. Fetch new one if necessary."
  []
  (log/info "Checking token")
  (let [valid-token (check-reddit-token)]
   (if valid-token
    valid-token
    (do
      (reset! reddit-token (fetch-reddit-token))
      (check-reddit-token)))))

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

(defn api-item-to-action
  [item]
  (let [occurred-at-iso8601 (str (coerce/from-long (* 1000 (long (-> item :data :created_utc)))))]
    {:id (DigestUtils/sha1Hex ^String (str "reddit-" (-> item :data :id)))
     :url (str "https://reddit.com" (-> item :data :permalink))
     :relation-type-id "discusses"
     :occurred-at occurred-at-iso8601
     :observations [{:type :url :input-url (-> item :data :url)}]
     :extra {
      :subreddit (-> item :data :subreddit)}
     :subj {
      :type (get work-types (:kind item) "unknown")
      :title (-> item :data :title)
      :issued occurred-at-iso8601}}))

; API
(defn parse-page
  "Parse response JSON to a page of Actions."
  [url json-data]
  (let [parsed (json/read-str json-data :key-fn keyword)]
    {:url url
     :extra {
      :after (-> parsed :data :after)
      :before (-> parsed :data :before)}
     :actions (map api-item-to-action (-> parsed :data :children))}))

(def auth-sleep-duration
  "Back off for a bit if we face an auth problem"
  ; 5 mins
  (* 1000 60 5))

(defn fetch-page
  "Fetch the API result, return a page of Actions."
  [domain after-token]
  (status/send! "reddit-agent" "reddit" "fetch-page" 1)
  (let [url (str "https://oauth.reddit.com/domain/" domain "/new.json?sort=new&after=" after-token)]
    ; If the API returns an error
    (try
      (try-try-again
        {:sleep 30000 :tries 10}
        #(let [result @(http/get url {:headers {"User-Agent" user-agent
                                               "Authorization" (str "bearer " (get-reddit-token))}})]
          (log/info "Fetched" url)

          (condp = (:status result)
            200 (parse-page url (:body result))
            404 {:url url :actions [] :extra {:after nil :before nil :error "Result not found"}}
            401 (do
                  (log/error "Unauthorized to access" url)
                  (log/error "Body of error response:" (:body url))
                  (log/info "Taking a nap...")
                  (Thread/sleep auth-sleep-duration)
                  (log/info "Woken up!")
                  (throw (new Exception "Unauthorized")))
            (do
              (log/error "Unexpected status code" (:status result) "from" url)
              (log/error "Body of error response:" (:body url))
              (throw (new Exception "Unexpected status"))))))

      (catch Exception ex (do
        (log/error "Error fetching" url)
        (log/error "Exception:" ex)
        {:url url :actions [] :extra {:after nil :before nil :error "Failed to retrieve page"}})))))


(def fetch-page-throttled (throttle-fn fetch-page 20 :minute))

(defn fetch-pages
  "Lazy sequence of pages for the domain."
  ([domain]
    (fetch-pages domain nil))

  ([domain after-token]
    (let [result (fetch-page-throttled domain after-token)
          ; Token for next page. If this is null then we've reached the end of the iteration.
          after-token (-> result :extra :after)]

      (if after-token
        (lazy-seq (cons result (fetch-pages domain after-token)))
        [result]))))

(defn all-action-dates-after?
  [date page]
  (let [dates (map #(-> % :occurred-at coerce/from-string) (:actions page))]
    (every? #(clj-time/after? % date) dates)))

(defn fetch-parsed-pages-after
  "Fetch seq parsed pages of Actions until all actions on the page happened before the given time."
  [domain date]
  (let [pages (fetch-pages domain)]
    (take-while (partial all-action-dates-after? date) pages)))

(defn check-all-domains
  "Check all domains for unseen links."
  [artifacts bundle-chan]
  (log/info "Start crawl all Domains on Reddit at" (str (clj-time/now)))
  (status/send! "reddit-agent" "process" "scan-domains" 1)
  (let [[domain-list-url domain-list] (get artifacts "domain-list")
        domains (clojure.string/split domain-list #"\n")
        ; Take 5 hours worth of pages to make sure we cover everything. The Percolator will dedupe.
        num-domains (count domains)
        counter (atom 0)
        cutoff-date (->  12  clj-time/hours clj-time/ago)]
    (doseq [domain domains]
      (swap! counter inc)
      (log/info "Query for domain:" domain @counter "/" num-domains " = " (int (* 100 (/ @counter num-domains))) "%")
      (let [pages (fetch-parsed-pages-after domain cutoff-date)
            package {:source-token source-token
                     :source-id "reddit"
                     :agent {:version version :artifacts {:domain-set-artifact-version domain-list-url}}
                     :extra {:cutoff-date (str cutoff-date) :queried-domain domain}
                     :pages pages}]
        (log/info "Sending package...")
        (>!! bundle-chan package))))
  (log/info "Finished scan."))

(def agent-definition
  {:agent-name "reddit-agent"
   :version version
   :schedule [{:name "check-all-domains"
              :seconds 14400 ; wait four hours between scans
              :fixed-delay true
              :fun check-all-domains
              :required-artifacts ["domain-list"]}]
   :runners []})

(defn -main [& args]
  (c/run agent-definition))
