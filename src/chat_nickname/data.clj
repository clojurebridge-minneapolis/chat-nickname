(ns chat-nickname.data
  (:import (com.eaio.uuid UUID))
  (:require [clj-time.local :as timelocal]))

(defn timestamp-now []
  (.getMillis (timelocal/local-now)))

(def data (atom {:servers {} :users {}}))

(def this-server-url (atom {}))

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

(defn update-server-timestamp [url timestamp]
  (put-server url (assoc (get-server url):updated timestamp)))

(defn update-this-server-timestamp []
  (update-server-timestamp @this-server-url (timestamp-now)))

(defn initialize-this-server [url version]
  (reset! this-server-url url)
  (put-server url {:version version}) ;; add it to the list of servers
  (update-this-server-timestamp)) ;; update the timestamp

(defn new-userid []
  (.toString (new UUID)))

(defn create-user [userid]
  (let [user {:name userid
              :server @this-server-url
              :said ""}]
    (put-user userid user)
    (update-this-server-timestamp)
    user))
