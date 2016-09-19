(ns event-data-reddit-agent.core
  (:require [org.crossref.event-data-agent-framework.core :as c]
            [org.crossref.event-data-agent-framework.util :as agent-util]
            [org.crossref.event-data-agent-framework.web :as agent-web]
            [crossref.util.doi :as cr-doi])
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :refer [reader]])
  (:require [clj-time.coerce :as coerce]
            [clj-time.core :as clj-time])
   (:gen-class))

(def source-token "a6c9d511-9239-4de8-a266-b013f5bd8764")
(def version "0.1.0")


(defn check-all-domains
  "Check all domain. "
  [artifacts send-evidence-callback]
  (log/info "Start crawl all Domains on Reddit at" (str (clj-time/now))))

(def agent-definition
  {:agent-name "reddit-agent"
   :version version
   :schedule [{:name "check-all-domains"
               :seconds 86400 ; 24 hours
               :fun check-all-domains
               :required-artifacts ["domain-list"]}]
   :runners []})

(defn -main [& args]
  (c/run args agent-definition))
