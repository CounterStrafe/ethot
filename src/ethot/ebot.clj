(ns ethot.ebot
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [clj-http.client :as hclient]
            [clj-http.conn-mgr :as conn-mgr]
            [config.core :refer [env]]
            [hickory.core :refer [as-hickory parse]]
            [hickory.select :as s]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs])
  (:gen-class))

(def state (atom {:cookies {}}))

(def cm (conn-mgr/make-reusable-conn-manager {}))

(def admin-user (:ebot-admin-user env))
(def admin-pass (:ebot-admin-pass env))
(def base-url (:ebot-base-url env))
(def db-name (:mysql-db env))
(def db-host (:mysql-host env))
(def db-user (:mysql-user env))
(def db-password (:mysql-pass env))
(def server-id-range (apply range (:server-id-range env)))

(def ds (jdbc/get-datasource
         {:dbtype "mysql"
          :dbname db-name
          :host db-host
          :user db-user
          :password db-password}))

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
        post-args (assoc get-args :connection-manager cm
                                  :cookies (:cookies @state)
                                  :form-params
                                    {"signin[username]" admin-user
                                     "signin[password]" admin-pass
                                     "signin[_csrf_token]" csrf})]
    (process-response (hclient/post url post-args))))

(defn import-game
  "Imports the game. Returns the eBot match ID."
  [tournament-id match-id game-number]
  (let [url (str base-url "/admin.php/matchs/toornament/import/" tournament-id "/" match-id "/" game-number)
        resp (process-response (hclient/post url {:connection-manager cm
                                                  :cookies (:cookies @state)}))]
    (get (json/read-str (:body resp)) "matchId")))

(defn export-game
  "Exports the game."
  [ebot-match-id]
  (let [url (str base-url "/admin.php/matchs/toornament/export/" ebot-match-id )]
    (process-response (hclient/post url {:connection-manager cm
                                         :cookies (:cookies @state)}))))

(defn assign-server
  "Assigns the server to the match."
  [server-id ebot-match-id ]
  (let [url (str base-url "/admin.php/matchs/current")]
    (process-response (hclient/post url {:connection-manager cm
                                         :cookies (:cookies @state)
                                         :form-params
                                           {"server_id" server-id
                                            "match_id" ebot-match-id}}))))

(defn start-match
  "Starts the match."
  [ebot-match-id]
  (let [url (str base-url "/admin.php/matchs/start/server")]
    (process-response (hclient/post url {:connection-manager cm
                                         :cookies (:cookies @state)
                                         :form-params
                                           {"match_id" ebot-match-id}}))))

(defn get-available-server
  "Returns the server ID of the next available server in ascending order."
  []
  (let [db {:dbtype "mysql"
            :dbname db-name
            :host db-host
            :user db-user
            :password db-password}
        ds (jdbc/get-datasource db)
        ; Match statuses below 13 indicate a game is either running or not started yet
        unavailable-servers (jdbc/execute-one! ds ["select distinct s.id
                                                    from servers s
                                                    left join matchs m
                                                    on s.id = m.server_id
                                                    where m.status < 13
                                                    order by s.id asc"]
                                               {:builder-fn rs/as-unqualified-lower-maps})]
        (first (apply sorted-set (clojure.set/difference server-id-range (set (:id unavailable-servers)))))))

(defn get-server-creds
  "Gets the server IP and password for the match. Returns map with keys [:ip :config_password]."
  [ebot-match-id]
  (jdbc/execute-one! ds ["select ip, config_password from matchs where id = ?" ebot-match-id]
                     {:builder-fn rs/as-unqualified-lower-maps}))

(defn get-newly-ended-games
  "Retrieves the games that have recently ended give the games we know already ended
  TODO: see if query can take a list directly for the not-in"
  [exportable-identifier-ids]
  (let [result (jdbc/execute! ds
                              [(str "select id from matchs where identifier_id in ("
                                    (str/join "," exportable-identifier-ids) ") "
                                    "and status >= 13")]
                              {:builder-fn rs/as-unqualified-lower-maps})]
    (map :id result)))

(defn get-match-id-with-team
  "Retrieves the games that have recently ended give the games we know already ended
  TODO: see if query can take a list directly for the not-in"
  [team-name]
  (let [result (jdbc/execute! ds
                              [(str "select id from matchs where "
                                    team-name " "
                                    "in (team_a_name, team_b_name)")]
                              {:builder-fn rs/as-unqualified-lower-maps})]
    (map :id result)))


(defn set-map
  "Sets the map for the match."
  [ebot-match-id map-name]
  (jdbc/execute-one! ds ["update maps
                            set map_name = ?
                            where match_id = ?" map-name, ebot-match-id]
                     {:builder-fn rs/as-unqualified-lower-maps}))
