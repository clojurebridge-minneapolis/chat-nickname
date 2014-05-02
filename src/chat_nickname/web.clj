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
            [environ.core :refer [env]]
            [clj-http.client :as client]
            [cheshire.core :as json]
            [clj-time.core :as dt]))

(defn new-userid []
  (let [userid (rand-int 1000000)] ;; you are one in a million!
    ;; check to make sure userid is not already in use!
    userid))

(defn timestamp-now []
  (.getMillis (dt/now)))

(def this-server {:url
                  (let [chat-url (:chat-url env)
                        port (:port env)]
                    (if chat-url
                      chat-url
                      (str "http://localhost:" (if port port 3000))))
                  :updated (timestamp-now)})

(def data (atom {:servers {(:url this-server) this-server}
                 :users {}}))

(defn get-servers []
  (get @data :servers))

(defn put-servers [servers]
  (swap! data assoc :servers servers))

(defn get-users []
  (get @data :users))

(defn put-users [users]
  (swap! data assoc :users users))

(defn get-server [url]
  (get (get-servers) url))

(defn put-server [url server]
  (put-servers (assoc (get-servers) url server)))

(defn get-user [userid]
  (get (get-users) userid))

(defn put-user [userid user]
  (put-users (assoc (get-users) userid user)))

(defn update-server [url timestamp]
  (let [server (get-server url)]
    (put-server url (assoc server :url url :updated timestamp))))

(defn update-other-server [o-url o-updated]
  (let [url (str o-url "/users")]
      (try
        (let [response (client/get url)
              status (:status response)
              body (if (== status 200) (:body response))
              other-users (if body (json/parse-string body true))]
          ;; (println "(update-other-server" o-url ") => " other-users)
          (doseq [kw-o-userid (keys other-users)]
            ;; NOTE json/parse-string will keywordize the userid
            (let [o-userid (Integer. (name kw-o-userid))
                  o-user (get other-users kw-o-userid)
                  o-server (:server o-user)]
              (if-not (get-server o-server)
                ;; if we have never seen this server, add it to the list to update next time
                (put-server o-server {:url o-server :updated 0}))
              (put-user o-userid o-user))))
        (catch Exception e
          (.getMessage e))))
  (update-server o-url o-updated))

(defn add-other-server [server]
  (let [url (str server "/servers")]
    (if (= server (:url this-server))
      "cannot add self!"
      (try
        (let [response (client/get url)
              status (:status response)
              body (if (== status 200) (:body response))
              other-servers (if body (json/parse-string body true))]
          ;; (println "(add-other-server" server ") => " other-servers)
          (doseq [kw-o-url (keys other-servers)]
            ;; NOTE json/parse-string will keywordize the url
            (let [o-url (subs (str kw-o-url) 1)]
              ;; (println "considering" o-url)
              (if (= o-url (:url this-server))
                nil ;; (println "ignoring adding self...")
                (let [o-server (get other-servers kw-o-url)
                      o-updated (:updated o-server)
                      local-server (get-server o-url)
                      local-updated (:updated local-server)]
                  (if (and local-server (= local-updated o-updated))
                    nil ;; (println "we are already up to date with" o-url)
                    (do
                      (println "need to update" o-url "%" o-updated " vs. " local-updated)
                      (update-other-server o-url o-updated)
                      )))))))
        (catch Exception e
          (.getMessage e))))))

(defn update-other-servers []
  ;; (println "getting updates from all OTHER servers...")
  (doseq [o-url (keys (get-servers))]
    (add-other-server o-url)))

(defn create-user [userid]
  (let [user {:name (str userid)
              :server (:url this-server)
              :said ""}]
    (put-user userid user)
    (update-server (:url this-server) (timestamp-now))
    user))

(defn unordered-list [coll & [class]]
  [(keyword (if class (str "ul." class) "ul"))
   (for [list-item coll]
     [:li list-item])])

(defn layout [title redirect & content]
  (let [delay (if (:production env) 0 3)]
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
  (let [nickname (if-let [n (:chat-nickname env)] n "nickname")
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
             (form/form-to [:post "/federate-server"]
                           "Join with everyone at URL: "
                           (form/text-field "server")
                           " "
                           (form/submit-button "federate"))
             [:br]
             (form/form-to [:post "/send-message"]
                           "Message: "
                           (form/text-field "message")
                           " "
                           (form/submit-button "send"))
             [:br]
             "What people are saying..." [:br]
             (unordered-list (for [u (keys (get-users))]
                               (let [user (get-user u)
                                     name (:name user)
                                     server (:server user)
                                     said (:said user)]
                                 (list [:b name] "@" server [:span.said said]))) "said")
             (if-not (:production env)
               (list
                [:div.debug
                 [:p [:b "DEBUG INFORMATION"]]
                 [:p "My userid = " userid ", userid-cookie = " userid-cookie]
                 [:p (str "data = " @data)]
                 "All the servers include" [:br]
                 (unordered-list (for [url (keys (get-servers))] (list [:b (str url)] ": " (str (get-server url)))))
                 "All the users include" [:br]
                 (unordered-list (for [u (keys (get-users))] (list [:b (str u)] ": " (str (get-user u)))))
                 "This is a " [:a {:href "/hello"} "Hello"] " link." [:br]
                 "This is a " [:a {:href "/stacktrace"} "Stacktrace"] " link." [:br]
                 "This is a " [:a {:href "/random"} "Missing"] " link." [:br]
                 "Return the list of " [:a {:href "/users"} "Users"] " as JSON." [:br]
                 "Return the list of " [:a {:href "/servers"} "Servers"] " as JSON." [:br]
                 ]
                ))
             )}))

(defn change-username [cookies params]
  (let [title "change username"
        userid-cookie (:value (cookies "userid"))
        userid (if userid-cookie (Integer. userid-cookie))
        user (if userid (get-user userid))
        username (:username params)]
    (layout title "/"
            [:h1 title]
            (if user
              (do
                (put-user userid (assoc user
                                   :name username
                                   :said (str (:name user) " is now known as " username)))
                (update-server (:url this-server) (timestamp-now))
                (update-other-servers)
                [:p (str "you changed your username (for userid = " userid ") to ") [:b username]])
              [:p "Invalid userid, please try again..."]))))

(defn federate-server [cookies params]
  (let [title "federate server"
        server (:server params)
        errmsg (add-other-server server)]
    (layout title "/"
            [:h1 title]
            (if errmsg
              [:p "Could not federate URL: " errmsg]
              [:p "you federated with the server @" [:b server]]))))

(defn send-message [cookies params]
  (let [title "send message"
        userid-cookie (:value (cookies "userid"))
        userid (if userid-cookie (Integer. userid-cookie))
        user (if userid (get-user userid))
        message (:message params)]
    (layout title "/"
            [:h1 title]
            (if user
              (if (str/blank? message)
                (do
                  (update-other-servers)
                  [:p "will just update the page..."])
                (do
                  (put-user userid (assoc user :said message))
                  (update-server (:url this-server) (timestamp-now))
                  (update-other-servers)
                  [:p "you sent message: " [:b message]]))
              [:p "Invalid userid, please try again..."]))))

(defroutes app-routes
  (GET "/" {cookies :cookies} (main-page cookies))
  (POST "/change-username" {cookies :cookies params :params} (change-username cookies params))
  (POST "/federate-server" {cookies :cookies params :params} (federate-server cookies params))
  (POST "/send-message" {cookies :cookies params :params} (send-message cookies params))
  (GET "/users" [] {:status 200
                    :headers {"Content-Type" "application/json; charset=utf-8"}
                    :body (json/generate-string (get-users))})
  (GET "/servers" [] {:status 200
                      :headers {"Content-Type" "application/json; charset=utf-8"}
                      :body (json/generate-string (get-servers))})
  (GET "/hello" [] "Hello World!")
  (GET "/stacktrace" [] ("string-is-not-a-function"))
  (route/resources "/")
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))

(def app
  (-> #'app-routes wrap-cookies wrap-keyword-params wrap-params))
