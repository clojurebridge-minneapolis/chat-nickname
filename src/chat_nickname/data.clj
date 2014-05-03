(ns chat-nickname.data
  (:require [environ.core :refer [env]]
            [clj-time.core :as dt])) ;; think of dt as Date and Time

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

(defn put-servers [servers]
  (swap! data assoc :servers servers))

(defn put-users [users]
  (swap! data assoc :users users))

(defn get-server [url]
  (get (:servers @data) url))

(defn put-server [url server]
  (put-servers (assoc (:servers @data) url server)))

(defn get-user [userid]
  (get (:users @data) userid))

(defn put-user [userid user]
  (put-users (assoc (:users @data) userid user)))

(defn update-server [url timestamp]
  (let [server (get-server url)]
    (put-server url (assoc server :url url :updated timestamp))))

(defn new-userid []
  (let [userid (rand-int 1000000)] ;; you are one in a million!
    ;; future option: check to make sure userid is not already in use
    userid))

(defn create-user [userid]
  (let [user {:name (str userid)
              :server (:url this-server)
              :said ""}]
    (put-user userid user)
    (update-server (:url this-server) (timestamp-now))
    user))
