(defproject chat-nickname "0.2.0-SNAPSHOT"
  :description "Sample Clojure web application"
  :url "https://github.com/clojurebridge-minneapolis/chat-nickname"
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}
  :min-lein-version "2.3.4"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.1.6"]
                 [hiccup "1.0.5"]
                 [environ "0.5.0"]]
                 ;; [ring/ring-jetty-adapter "1.1.0"]
                 ;; [ring/ring-devel "1.1.0"]
                 ;; [ring-basic-authentication "1.0.1"]
                 ;; [com.cemerick/drawbridge "0.0.6"]]
  :plugins [[lein-ring "0.8.10"]
            [lein-environ "0.5.0"]]
  :ring {:handler chat-nickname.web/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}
   :production {:env {:production true}}})
