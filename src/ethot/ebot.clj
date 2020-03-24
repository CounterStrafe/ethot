(ns ethot.ebot
  (:require [clojure.string :as str]
            [clj-http.client :as hclient]
            [clj-http.conn-mgr :as conn-mgr]
            [config.core :refer [env]]
            [hickory.core :refer [as-hickory parse]]
            [hickory.select :as s])
  (:gen-class))

(def state (atom {:cookies {}}))

(def cm (conn-mgr/make-reusable-conn-manager {}))

(def admin-user (:ebot-admin-user env))
(def admin-pass (:ebot-admin-pass env))
(def base-url (:ebot-base-url env))

(defn process-response
  "Updates the atom with the new cookies in the response if they exist. Returns
  the response."
  [resp]
  (if (contains? resp :cookies) (swap! state assoc :cookies (:cookies resp)))
  resp)

(defn get-admin-page
  "Returns the admin home page."
  []
  (let [url (str base-url "/admin.php/")]
    ; Don't throw exceptions because this page will return a 401
    (process-response (hclient/get url {:connection-manager cm
                                        :throw-exceptions false
                                        :cookies (:cookies @state)}))))

(defn login
  "Logs into the eBot home page. Returns the resposne."
  []
  (let [url (str base-url "/admin.php/guard/login")
        ; Don't throw exceptions because this page will return a 401
        get-args {:connection-manager cm :throw-exceptions false}
        get-resp (get-admin-page)
        htree (as-hickory (parse (:body get-resp)))
        csrf (-> (s/id :signin__csrf_token)
                 (s/select htree)
                 first :attrs :value)
        post-args (assoc get-args :cookies (:cookies @state)
                                  :form-params
                                    {"signin[username]" admin-user
                                     "signin[password]" admin-pass
                                     "signin[_csrf_token]" csrf})]
    (process-response (hclient/post url post-args))))

(defn import-game
  "Imports the game. Returns the response."
  [tournament-id match-id game-number]
  (let [import-url (str base-url "/admin.php/matchs/toornament/import/" tournament-id "/" match-id "/" game-number)]
    (process-response (hclient/post import-url {:connection-manager cm
                                                :throw-exceptions false
                                                :cookies (:cookies @state)}))))
