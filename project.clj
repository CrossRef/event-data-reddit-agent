(defproject event-data-reddit-agent "0.1.3"
  :description "Crossref Event Data Reddit.com Agent"
  :url "http://eventdata.crossref.org"
  :license {:name "The MIT License (MIT)"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.crossref.event-data-agent-framework "0.1.12"]
                 [event-data-common "0.1.14"]
                 [throttler "1.0.0"]
                 [http-kit "2.1.18"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [commons-codec/commons-codec "1.10"]]
  :main ^:skip-aot event-data-reddit-agent.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :prod {:resource-paths ["config/prod"]}
             :dev  {:resource-paths ["config/dev"]}})
