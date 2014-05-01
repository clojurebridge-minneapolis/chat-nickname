(ns chat-nickname.web
  (:use [compojure.core]
        [ring.middleware.cookies        :only [wrap-cookies]]
        [ring.middleware.params         :only [wrap-params]]
        [ring.middleware.keyword-params :only [wrap-keyword-params]])
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [compojure.route :as route]
            [hiccup.core :as hiccup]
            [hiccup.page :as page]
            [hiccup.form :as form]
            [environ.core :refer [env]]))

(defonce users (atom {}))

(defn new-userid []
  (let [userid (rand-int 10)]
    userid))

(defn get-user [userid]
  (get @users userid))

(defn put-user [userid user]
  (swap! users assoc userid user))

(defn create-user [userid]
  (let [user {:name (str userid)
              :server nil
              :said ""}]
    (put-user userid user)
    user))

(defn unordered-list
  [coll]
  [:ul
   (for [list-item coll]
     [:li list-item])])

(defn layout [title redirect & content]
  (let [delay (if (:production env) 0 5)]
    (page/html5
     [:head
      [:title title]
      (page/include-css "/css/chat.css")
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport"
              :content "width=device-width,initial-scale=1.0,maximum-scale=5.0,minimum-scale=0.2,user-scalable=1"}]
      (if redirect
        [:meta {:http-equiv "refresh" :content (str delay "; url=" redirect)}])
      ]
     [:body
      [:div.container
       content]])))

(defn main-page [cookies]
  (let [nickname (if-let [n (:nickname env)] n "nickname")
        chat-nickname (str "chat-" nickname)
        title (if (:production env) chat-nickname (str chat-nickname " :dev"))
        userid-cookie (:value (cookies "userid"))
        userid (if userid-cookie (Integer. userid-cookie) (new-userid))
        user (if-let [u (get-user userid)] u (create-user userid))]
    {:cookies {"userid" userid}
     :body
     (layout title nil
            [:h1 title]
            [:p "This is an example Clojure application for chatting with your friends."]
            (form/form-to [:post "/change-username"]
                          "My username is: "
                          (form/text-field "username" (:name user))
                          " "
                          (form/submit-button "change"))
            [:br]
            (form/form-to [:post "/send-message"]
                          "Message: "
                          (form/text-field "message")
                          " "
                          (form/submit-button "send"))
            [:br]
            "What people are saying..." [:br]
            (unordered-list (for [u (keys @users)]
                              (let [user (get-user u)]
                                (list [:b (:name user)] " " (:said user)))))
            (if-not (:production env)
              (list
               [:div.debug
                [:p [:b "DEBUG INFORMATION"]]
                [:p "My userid = " userid ", userid-cookie = " userid-cookie]
                "All the users include" [:br]
                 (unordered-list (for [u (keys @users)] (list [:b (str u)] ": " (str (get-user u)))))
                "This is a " [:a {:href "/hello"} "Hello"] " link." [:br]
                "This is a " [:a {:href "/stacktrace"} "Stacktrace"] " link." [:br]
                "This is a " [:a {:href "/random"} "Missing"] " link." [:br]
                ]
               ))
            )}))

(defn change-username [cookies params]
  (let [title "change username"
        userid-cookie (:value (cookies "userid"))
        userid (if userid-cookie (Integer. userid-cookie))
        username (:username params)]
    (layout title "/"
            [:h1 title]
            (if userid
              (let [user (get-user userid)]
                (put-user userid (assoc user
                                   :name username
                                   :said (str (:name user) " is now known as " username)))
                [:p (str "you changed your username (for userid = " userid ") to ") [:b username]])
              [:p "Invalid userid, please try again..."]))))

(defn send-message [cookies params]
  (let [title "send message"
        userid-cookie (:value (cookies "userid"))
        userid (if userid-cookie (Integer. userid-cookie))
        message (:message params)]
    (layout title "/"
            [:h1 title]
            (if userid
              (if (str/blank? message)
                [:p "will just update the page..."]
                (let [user (get-user userid)]
                  (put-user userid (assoc user :said message))
                  [:p "you sent message: " [:b message]]))
              [:p "Invalid userid, please try again..."]))))

(defroutes app-routes
  (GET "/" {cookies :cookies} (main-page cookies))
  (POST "/change-username" {cookies :cookies params :params} (change-username cookies params))
  (POST "/send-message" {cookies :cookies params :params} (send-message cookies params))
  (GET "/hello" [] "Hello World!")
  (GET "/stacktrace" [] ("string-is-not-a-function"))
  (route/resources "/")
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))

(def app
  (-> #'app-routes wrap-cookies wrap-keyword-params wrap-params))
