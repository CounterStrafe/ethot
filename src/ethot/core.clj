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
(def toornament-api-key (:toornament-api-key env))
(def toornament-client-id (:toornament-client-id env))
(def toornament-client-secret (:toornament-client-secret env))
(def toornament-url "https://api.toornament.com")

(defn toornament-oauth
  [scope]
  (let [url (str toornament-url "/oauth/v2/token")]
    (get (:body (hclient/post url {:form-params {:grant_type "client_credentials"
                                                 :client_id toornament-client-id
                                                 :client_secret toornament-client-secret
                                                 :scope (str "organizer:" scope)}})
      "access_token"))))

(defn toornament-tournaments
  []
  (let [url (str toornament-url "/organizer/v2/tournaments")]
    (hclient/get url {:headers {:X-Api-Key toornament-api-key
                                :Authorization (toornament-oauth "view")}})))

(defn ebot-login
  []
  (let [url (str ebot-url "/admin.php/guard/login")
        ; Don't throw exceptions because this page will return a 401
        get-args {:connection-manager ebot-cm :throw-exceptions false}
        get-resp (hclient/get url get-args)
        htree (as-hickory (parse (:body get-resp)))
        csrf (-> (s/select (s/id :signin__csrf_token) htree)
                 first :attrs :value)
        post-args (assoc get-args :cookies (:cookies get-resp) :form-params {
          "signin[username]" ebot-admin-user
          "signin[password]" ebot-admin-pass
          "signin[_csrf_token]" csrf})]
    (hclient/post url post-args)))

(defn -main
  [& args]
  (let [cookies (:cookies (ebot-login))]
    (println (hclient/get (str ebot-url "/admin.php/guard/login") {:connection-manager ebot-cm :cookies cookies}))))
