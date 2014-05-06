(ns chat-nickname.services
  (:use [chat-nickname.data])
  (:require [clojure.string :as str]
            [environ.core :refer [env]]
            [hiccup.core :as hiccup]
            [hiccup.page :as page]
            [hiccup.form :as form]
            [clj-http.client :as client]
            [cheshire.core :as json]))

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
        userid (if userid-cookie userid-cookie (new-userid))
        user (if-let [u (get-user userid)] u (create-user userid))]
    {:cookies {"userid" userid}
     :body
     (layout
      title nil
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
      (unordered-list (for [u (keys (:users @data))]
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
          (unordered-list (for [url (keys (:servers @data))]
                            (list [:b (str url)] ": " (str (get-server url)))))
          "All the users include" [:br]
          (unordered-list (for [u (keys (:users @data))]
                            (list [:b (str u)] ": " (str (get-user u)))))
          "This is a " [:a {:href "/hello"} "Hello"] " link." [:br]
          "This is a " [:a {:href "/stacktrace"} "Stacktrace"] " link." [:br]
          "This is a " [:a {:href "/random"} "Missing"] " link." [:br]
          "Return the list of " [:a {:href "/users"} "Users"] " as JSON." [:br]
          "Return the list of " [:a {:href "/servers"} "Servers"] " as JSON." [:br]
          ]
         ))
      )}))

(defn update-other-server [o-url o-updated]
  (let [url (str o-url "/users")]
      (try
        (let [response (client/get url)
              status (:status response)
              body (if (== status 200) (:body response))
              other-users (if body (json/parse-string body true))]
          (doseq [kw-o-userid (keys other-users)]
            ;; NOTE json/parse-string will keywordize the userid
            (let [o-userid (Integer. (name kw-o-userid))
                  o-user (get other-users kw-o-userid)
                  o-server (:server o-user)]
              (if-not (get-server o-server)
                ;; if we have never seen this server...
                ;; add it to the list to update next time
                (put-server o-server {:updated 0}))
              (put-user o-userid o-user))))
        (catch Exception e
          (.getMessage e))))
  (update-server-timestamp o-url o-updated))

(defn add-other-server [server]
  (let [url (str server "/servers")]
    (if (= server @this-server-url)
      "cannot add self!"
      (try
        (let [response (client/get url)
              status (:status response)
              body (if (== status 200) (:body response))
              other-servers (if body (json/parse-string body true))]
          (doseq [kw-o-url (keys other-servers)]
            ;; NOTE json/parse-string will keywordize the url
            (let [o-url (subs (str kw-o-url) 1)]
              (if (= o-url @this-server-url)
                nil ;; (println "ignoring adding self...")
                (let [o-server (get other-servers kw-o-url)
                      o-updated (:updated o-server)
                      local-server (get-server o-url)
                      local-updated (:updated local-server)]
                  (if (and local-server (= local-updated o-updated))
                    nil ;; (println "we are already up to date with" o-url)
                    (do
                      ;; (println "need to update" o-url "%" o-updated " vs. " local-updated)
                      (update-other-server o-url o-updated)
                      )))))))
        (catch Exception e
          (.getMessage e))))))

(defn update-other-servers []
  ;; (println "getting updates from all OTHER servers...")
  (doseq [o-url (keys (:servers @data))]
    (add-other-server o-url)))


(defn change-username [cookies params]
  (let [title "change username"
        userid (:value (cookies "userid"))
        user (if userid (get-user userid))
        username (:username params)]
    (layout title "/"
            [:h1 title]
            (if user
              (do
                (put-user userid
                          (assoc user
                            :name username
                            :said (str (:name user)
                                       " is now known as " username)))
                (update-this-server-timestamp)
                (update-other-servers)
                [:p (str "you changed your username (for userid = "
                         userid ") to ") [:b username]])
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
        userid (:value (cookies "userid"))
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
                  (update-this-server-timestamp)
                  (update-other-servers)
                  [:p "you sent message: " [:b message]]))
              [:p "Invalid userid, please try again..."]))))
