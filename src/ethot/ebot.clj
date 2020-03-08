(ns ethot.ebot
  (:require [clojure.string :as str]
            [clj-http.client :as hclient]
            [clj-http.conn-mgr :as conn-mgr]
            [config.core :refer [env]]
            [hickory.core :refer [as-hickory parse]]
            [hickory.select :as s])
  (:gen-class))

(def ebot-cm (conn-mgr/make-reusable-conn-manager {}))

(def ebot-admin-user (:ebot-admin-user env))
(def ebot-admin-pass (:ebot-admin-pass env))
(def ebot-url (:ebot-url env))

(defn ebot-login
  []
  (let [url (str ebot-url "/admin.php/guard/login")
        ; Don't throw exceptions because this page will return a 401
        get-args {:connection-manager ebot-cm :throw-exceptions false}
        get-resp (hclient/get url get-args)
        htree (as-hickory (parse (:body get-resp)))
        csrf (-> (s/id :signin__csrf_token)
                 (s/select htree)
                 first :attrs :value)
        post-args (assoc get-args :cookies (:cookies get-resp)
                                  :form-params
                                    {"signin[username]" ebot-admin-user
                                     "signin[password]" ebot-admin-pass
                                     "signin[_csrf_token]" csrf})]
    (hclient/post url post-args)))
