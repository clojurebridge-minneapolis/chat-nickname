(defproject chat-nickname "0.2.4"
  :description "Sample Clojure web application"
  :url "https://github.com/clojurebridge-minneapolis/chat-nickname"
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}
  :min-lein-version "2.3.4"
  :plugins [[lein-environ "0.5.0"]] ;; allows :env below
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.1.6"]
                 [ring/ring-jetty-adapter "1.2.2"]
                 [ring-basic-authentication "1.0.5"]
                 [com.cemerick/drawbridge "0.0.6"]
                 [hiccup "1.0.5"]
                 [environ "0.5.0"]
                 [javax.servlet/servlet-api "2.5"]
                 [clj-http "0.9.1"]
                 [cheshire "5.3.1"]
                 [clj-time "0.4.4"]]
  :main chat-nickname.web
  :profiles {
             :shared {:env {:chat-nickname-version ""}} ;; set version env var
             :dev [:shared {:dependencies [[ring-mock "0.1.5"]]}]
             :production [:shared {:env {:production true}}]
             })
