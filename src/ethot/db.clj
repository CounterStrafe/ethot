(ns ethot.db
  (:require [config.core :refer [env]]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs])
  (:gen-class))

(def db-name "ethot")
(def db-host (:mysql-host env))
(def db-user (:mysql-user env))
(def db-password (:mysql-pass env))

(def ds (jdbc/get-datasource
         {:dbtype "mysql"
          :dbname db-name
          :host db-host
          :user db-user
          :password db-password}))

(defn create-veto-lobby
  "Creates a veto lobby in the database."
  [match-id tournament-id ebot-match-id team1 team2 discord-channel-id]
  (let [team1-id (get team1 "id")
        team2-id (get team2 "id")
        next-ban (first (shuffle (list team1 team2)))
        next-ban-id (get next-ban "id")]
    (jdbc/execute-one! ds ["insert into veto (match_id, tournament_id, ebot_match_id, team1_id, team2_id, discord_channel_id, next_ban_id)
                            values (?, ?, ?, ?, ?, ?, ?)"
                           match-id tournament-id ebot-match-id team1-id team2-id discord-channel-id next-ban-id]
                       {:builder-fn rs/as-unqualified-lower-maps})
    next-ban))

(defn get-veto-lobby
  "Gets a veto lobby from the database from it's discord channel id."
  [discord-channel-id]
  (jdbc/execute-one! ds ["select * from veto
                          where discord_channel_id = ?
                          and active = 1" discord-channel-id]
                     {:builder-fn rs/as-unqualified-lower-maps}))

(defn ban-map
  "Bans a map in a veto lobby."
  [match-id map-name next-ban-id]
  (jdbc/execute-one! ds [(str "update veto "
                              "set " map-name " = 1, "
                              "next_ban_id = " next-ban-id " "
                              "where match_id = " match-id)]
                     {:builder-fn rs/as-unqualified-lower-maps}))

(defn end-veto
  "Marks a veto lobby inactive."
  [match-id]
  (jdbc/execute-one! ds ["update veto
                          set active = 0
                          where match_id = ?" match-id]
                     {:builder-fn rs/as-unqualified-lower-maps}))

(defn delay-match
  "Adds a match to the delays table."
  [match-id]
  (jdbc/execute-one! ds ["insert into delays (match_id)
                          values (?)" match-id]
                     {:builder-fn rs/as-unqualified-lower-maps}))

(defn match-delayed?
  "Checks if a match is delayed."
  [match-id]
  (not= (:c (jdbc/execute-one! ds ["select count(*) as c
                                    from delays
                                    where match_id = ?" match-id]
                               {:builder-fn rs/as-unqualified-lower-maps}))
        0))

(defn resume-match
  "Removes a match from the delays table."
  [match-id]
  (jdbc/execute-one! ds ["delete from delays
                          where match_id = ?" match-id]
                     {:builder-fn rs/as-unqualified-lower-maps}))
