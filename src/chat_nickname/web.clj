(ns chat-nickname.web
  (:use [chat-nickname.data]
        [chat-nickname.services])
  (:require [clojure.java.io :as io]
            [environ.core :refer [env]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.params  :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.nested-params :refer [wrap-nested-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.basic-authentication :refer [wrap-basic-authentication]]
            [compojure.core :refer [defroutes GET POST ANY]]
            [compojure.route :as route]
            [ring.adapter.jetty :as jetty]
            [cemerick.drawbridge :as drawbridge]
            [cheshire.core :as json]))

(defroutes app-routes
  (GET "/" {cookies :cookies}
       (main-page cookies))
  (POST "/change-username" {cookies :cookies params :params}
        (change-username cookies params))
  (POST "/federate-server" {cookies :cookies params :params}
        (federate-server cookies params))
  (POST "/send-message" {cookies :cookies params :params}
        (send-message cookies params))
  (GET "/users" [] {:status 200
                    :headers {"Content-Type" "application/json; charset=utf-8"}
                    :body (json/generate-string (:users @data))})
  (GET "/servers" [] {:status 200
                      :headers {"Content-Type" "application/json; charset=utf-8"}
                      :body (json/generate-string (:servers @data))})
  (GET "/hello" []
       "Hello World!")
  (GET "/stacktrace" []
       ("string-is-not-a-function"))
  (route/resources "/")
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))

(def app
  (-> #'app-routes wrap-keyword-params wrap-nested-params wrap-params wrap-cookies wrap-session))

(def drawbridge-handler
  (-> (drawbridge/ring-handler)
      (wrap-keyword-params)
      (wrap-nested-params)
      (wrap-params)
      (wrap-session)))

(defn authenticated? [name pass]
  (= [name pass] [(:chat-nickname env) (:repl-password env)]))

(defn wrap-drawbridge [handler]
  (fn [req]
    (let [handler (if (= "/repl" (:uri req))
                    (wrap-basic-authentication
                     drawbridge-handler authenticated?)
                    handler)]
      (handler req))))

(defn -main [& [port]]
  (let [port (Integer. (or port (:port env) 3000))
        chat-nickname-version (or (:chat-nickname-version env) "unknown")
        chat-url (or (:chat-url env) (str "http://localhost:" port))]
    (initialize-this-server chat-url chat-nickname-version)
    (println "@this-server-url" @this-server-url)
    (println "@data" @data)
    (jetty/run-jetty (wrap-drawbridge app)
                     {:port port :join? false})))
