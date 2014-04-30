(ns chat-nickname.web
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [hiccup.core :as hiccup]
            [hiccup.page :as page]
            [hiccup.form :as form]
            [environ.core :refer [env]]))

(defn layout
  [title & content]
  (page/html5
   [:head
    [:title title]
    (page/include-css "/css/chat.css")
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport" :content "width=device-width,initial-scale=1.0,maximum-scale=5.0,minimum-scale=0.2,user-scalable=1"}]]
   [:body
    [:div.container
     content]]))

;; this function is currently only used for testing on the repl
(defn authenticated? [user pass]
  ;; TODO: heroku config:add NICKNAME=[...] REPL_PASSWORD=[...]
  (= [user pass] [(env :nickname false) (env :repl-password false)]))

(defn main-page []
  (let [nickname (if-let [n (:nickname env)] n "nickname")
        chat-nickname (str "chat-" nickname)
        title (if (:production env) chat-nickname (str chat-nickname " :dev"))]
    (layout title
            [:h1 title]
            [:p "This is a demo application."]
            "This is a " [:a {:href "/hello"} "Hello"] " link."
            )))

(defroutes app-routes
  (GET "/" [] (main-page))
  (GET "/hello" [] "Hello World!")
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
