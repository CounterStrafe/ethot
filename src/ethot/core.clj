(ns ethot.core
  (:require [discljord.connections :as dconn]
            [discljord.messaging :as dmess]
            [discljord.events :as devent]
            [clojure.core.async :as async]
            [clojure.string :as str]
            [clj-http.client :as hclient]
            [clj-http.conn-mgr :as conn-mgr]
            [config.core :refer [env]])
  (:gen-class))

(def state (atom {}))
(def ebot-cm (conn-mgr/make-reusable-conn-manager {}))

(def apik (:toornament-api-key env))
(def ebot-admin-user (:ebot-admin-user env))
(def ebot-admin-pass (:ebot-admin-pass env))
(def ebot-url (:ebot-url env))

(defn ebot-login []
  (let [
    ebot-login-url (str ebot-url "/admin.php/guard/login")
    post-data {}])
  (hclient/post ebot-login-url))

(defn -main
  [& args]
  (println (str (hclient/get "http://www.google.com" {:connection-manager ebot-cm}))))
