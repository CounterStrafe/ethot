(ns ethot.core
  (:require [discljord.connections :as dconn]
            [discljord.messaging :as dmess]
            [discljord.events :as devent]
            [clojure.core.async :as async]
            [clojure.string :as str]
            [clj-http.client :as hclient]
            [clj-http.conn-mgr :as conn-mgr]
            [config.core :refer [env]]
            [hickory.core :refer [as-hickory parse]]
            [hickory.select :as s])
  (:gen-class))

(def state (atom {}))
(def ebot-cm (conn-mgr/make-reusable-conn-manager {}))

(def apik (:toornament-api-key env))
(def ebot-admin-user (:ebot-admin-user env))
(def ebot-admin-pass (:ebot-admin-pass env))
(def ebot-url (:ebot-url env))

(defn ebot-login
  []
  (let [ebot-login-url (str ebot-url "/admin.php/guard/login")
        ebot-login-htree (->
          ; Don't throw exceptions because this page will return a 401
          (hclient/get ebot-login-url {:connection-manager ebot-cm :throw-exceptions false})
          :body parse as-hickory)]
          (-> (s/select (s/id :signin__csrf_token) ebot-login-htree)
              first :attrs :value
          )))

(defn -main
  [& args]
  (println (ebot-login)))
