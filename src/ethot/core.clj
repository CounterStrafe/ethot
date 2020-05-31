(ns ethot.core
  (:require [discljord.connections :as dconn]
            [discljord.messaging :as dmess]
            [discljord.events :as devent]
            [clojure.core.async :as async]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [clj-http.client :as hclient]
            [config.core :refer [env]]
            [ethot.db :as db]
            [ethot.ebot :as ebot]
            [ethot.toornament :as toornament])
  (:gen-class))

(def state (atom {}))

(def discord-admin-channel-id (:discord-admin-channel-id env))
(def discord-announcements-channel-id (:discord-announcements-channel-id env))
(def discord-guild-id (:discord-guild-id env))
(def discord-server-channel-ids (:discord-server-channel-ids env))
(def discord-token (:discord-token env))
(def game-server-password (:game-server-password env))
(def map-pool (:map-pool env))

(defn gen-discord-user-map
  "Generates a map of Discord usernames to their ID's."
  []
  (reduce (fn [user-map user]
            (assoc user-map
                   (str (get-in user [:user :username]) "#" (get-in user [:user :discriminator]))
                   (get-in user [:user :id])))
          {}
          @(dmess/list-guild-members! (:messaging @state)
                                      discord-guild-id
                                      :limit 1000)))

(defn get-discord-user-id
  "Takes a Discord username and returns it's Discord ID."
  [discord-username]
  (when (not (contains? (:discord-user-ids @state) discord-username))
    (swap! state assoc :discord-user-ids (gen-discord-user-map)))
  (get (:discord-user-ids @state) discord-username))

(defn get-team-discord-usernames
  "Takes a Toornament team and returns the discord usernames in it."
  [team]
  (map #(str/trim (get-in % ["custom_fields" "discord_username"])) (get team "lineup")))

(defn format-discord-mentions
  "Takes a sequence of Discord ID's and retuns a string that mentions them."
  [discord-ids]
  (str/join " " (map #(str "<@" % ">") discord-ids)))

(defn notify-discord
  "Announces the game in the announcements channel and DM's all the players with
  the server creds."
  [tournament-id team1 team2 server-id]
  (let [team1-name (get team1 "name")
        team2-name (get team2 "name")
        discord-channel-id (get discord-server-channel-ids (- server-id 1))
        discord-usernames (concat (get-team-discord-usernames team1)
                                  (get-team-discord-usernames team2))
        discord-ids (map #(get-discord-user-id %) discord-usernames)]
    (dmess/create-message! (:messaging @state) discord-channel-id
                           :content (str team1-name " vs " team2-name " is now ready!"
                                         "\n" (format-discord-mentions discord-ids)))
    (dmess/create-message! (:messaging @state) discord-announcements-channel-id
                           :content (str team1-name " vs " team2-name " is now ready "
                                         "on <#" discord-channel-id ">"))))

(defn unimported-matches
  "Returns the matches that can and have not been imported yet."
  [tournament-id]
  (filter #(and (not (ebot/imported? tournament-id (get % "id") 1))
                (not (db/match-delayed? (get % "id"))))
          (toornament/importable-matches tournament-id)))

(defn await-game-status
  "waits for the channel to recieve a map of inforamiotn about the caller
  or nil from timeout and will the find the game to delay exporting and make sure"
  [id time-to-wait chan]
  (println (str "testing chan passed to await-game-status" chan))
  (async/go
    (async/alt!
      (async/timeout time-to-wait) ([x]
                                    (ebot/export-game id)
                                    (db/set-exported id))
      chan ([x] (db/set-reported id)))))

(defn export-games
  "Will find new games that have recently ended and create a new channel that
  will be notified when either the 5min timeout is reached to allow the next
  game in the bracket to be started OR a player reported suspicious activity
  and will stop the starting of the next game until manually restarted by a TO"
  [state tournament-id]
  (let [{:keys [games-awaiting-close close-game-time]} state
        ready-games (toornament/importable-matches tournament-id)
        identifier-ids (map #(str "'" tournament-id
                                  "."
                                  (get % "id")
                                  "."
                                  1
                                  "'") ready-games)
        recently-ended (ebot/get-newly-ended-games identifier-ids)]
    (doseq [ebot-id (filter #(and (contains? games-awaiting-close (str %))
                                  (not (db/report-timer-started? %))) recently-ended)]
      (db/set-report-timer ebot-id)
      (await-game-status ebot-id close-game-time (get-in state [:games-awaiting-close (str ebot-id)])))))

(defn start-veto
  "Creates a veto lobby state and notifies the Discord server channel that the
  veto has started."
  [tournament-id match-id ebot-match-id server-id team1 team2]
  (let [discord-channel-id (get discord-server-channel-ids (- server-id 1))
        team1-name (get team1 "name")
        team2-name (get team2 "name")
        first-to-ban (db/create-veto-lobby match-id tournament-id ebot-match-id team1 team2 discord-channel-id)]
    (dmess/create-message! (:messaging @state) discord-channel-id
                           :content (str "The map pool is:\n" (str/join "\n" map-pool)
                                         "\n\nTeams will alternate bans until one map remains. **"
                                         (get first-to-ban "name") "** will be the first to ban."
                                         "\nPlease use `!ban <mapname>` to ban a map."))))

(defn get-team-of-discord-user
  "given the discord username will find the team on toornament they belong to"
  [discord-username]
  (let [tournament-id (:tournament-id @state)
        participants (toornament/participants tournament-id)]
    ;;could be much cleaner with a postwalk, but this is more performant
    (some (fn [m]
            (when
                (some
                 #(= % discord-username)
                 (map #(get-in % ["custom_fields" "discord_username"])
                      (get m "lineup")))
              (get m "name")))
          participants)))

(defn end-veto
  "Sets the map in eBot, notifies the Discord server channel that the veto has
  ended, and DM the server creds to the players."
  [match-id veto-lobby banned-map map-name ban-team team1 team2]
  (let [channel-id (:discord_channel_id veto-lobby)
        teams (list team1 team2)
        team1-name (get (first teams) "name")
        team2-name (get (second teams) "name")
        discord-usernames (flatten (map get-team-discord-usernames teams))
        discord-user-ids (map get-discord-user-id discord-usernames)
        ebot-match-id (:ebot_match_id veto-lobby)
        {:keys [ip config_password]} (ebot/get-server-creds ebot-match-id)]
    (ebot/set-map ebot-match-id map-name)
    (ebot/start-match ebot-match-id)
    (db/end-veto match-id)
    (dmess/create-message! (:messaging @state) channel-id
                            :content (str (get ban-team "name") " banned "
                                          banned-map ". **" map-name
                                          "** will be played."))
    (doseq [discord-id discord-user-ids]
      (let [channel-id (:id @(dmess/create-dm! (:messaging @state) discord-id))]
        (dmess/create-message! (:messaging @state) channel-id
                               :content (str team1-name " vs " team2-name " is now ready!"
                                             "\nsteam://connect/" ip "/" config_password
                                             "\n" "`connect " ip
                                             "; password " config_password ";`"))))))

(defn get-maps-left
  "Returns the maps left in a veto lobby."
  [veto-lobby]
  (filter (fn [key]
            (and (contains? (set map-pool) key)
                 (= (get veto-lobby (keyword key)) "0")))
          (map name (keys veto-lobby))))

(defn ban-map
  "Removes the map from the veto lobby's remaining maps, updates the ban order,
  and updates the server's Discord channel."
  [veto-lobby map-name team1 team2]
  (let [maps-left (filter #(not (= map-name %)) (get-maps-left veto-lobby))
        match-id (:match_id veto-lobby)
        channel-id (:discord_channel_id veto-lobby)
        next-ban-id (:next_ban_id veto-lobby)
        [ban-team next-ban-team] (if (= next-ban-id (get team1 "id"))
                                   [team1 team2]
                                   [team2 team1])]
    (db/ban-map match-id map-name (get next-ban-team "id"))
    (if (= (count maps-left) 1)
      (end-veto match-id veto-lobby map-name (first maps-left) ban-team team1 team2)
      (dmess/create-message! (:messaging @state) channel-id
                              :content (str (get ban-team "name") " banned "
                                            map-name ". **" (get next-ban-team "name")
                                            "** will ban next. Maps remaining:\n"
                                            (str/join "\n" maps-left))))))

(defn run-stage
  "Logs into eBot and continuously imports and exports all available games
  every 30 seconds."
  [tournament-name stage-name]
  (async/go
    (ebot/login)
    (let [tournament-id (get (toornament/get-tournament tournament-name) "id")
          stage-id (get (toornament/get-stage tournament-id stage-name) "id")]
      (swap! state assoc :tournament-id tournament-id)
      (loop []
        (println "Running")
        (doseq [match (unimported-matches tournament-id)]
          (let [match-id (get match "id")
                ; Currently we only support single-game matches
                game (first (toornament/games tournament-id match-id))
                game-number (get game "number")
                ebot-match-id (ebot/import-game tournament-id match-id game-number)
                team1-id (get-in match ["opponents" 0 "participant" "id"])
                team2-id (get-in match ["opponents" 1 "participant" "id"])
                team1 (toornament/participant tournament-id team1-id)
                team2 (toornament/participant tournament-id team2-id)
                ; This code assumes there are more available servers than games
                ; that can be played at one time. There is no logic for
                ; prioritising games earlier in the bracket.
                server-id (ebot/get-available-server)]
            (ebot/set-match-password ebot-match-id game-server-password)
            (ebot/assign-server server-id ebot-match-id)
            (notify-discord tournament-id team1 team2 server-id)
            (start-veto tournament-id match-id ebot-match-id server-id team1 team2)
            (when (not (contains? (:games-awaiting-close @state) ebot-match-id))
              (swap! state assoc-in [:games-awaiting-close ebot-match-id] (async/chan))
              (cond
                (not (db/in-reports-table? ebot-match-id))
                (db/add-unreported ebot-match-id)

                ;; we will reset the timer, since the game will
                ;; still be exportable on through toornament state
                (db/in-timer? ebot-match-id)
                (do
                  (println "recovered game that was in timer")
                  (db/set-unreported ebot-match-id))))))

                                        ;exports here
        (export-games @state tournament-id)
        (async/<! (async/timeout 30000))
        (if (or (not (:stage-running @state))
                (toornament/stage-complete? tournament-id stage-id))
          (println "Stopping")
          (recur))))))

(defmulti handle-event
  (fn [event-type event-data]
    (when (and
           (not (:bot (:author event-data)))
           (= event-type :message-create))
      (first (str/split (:content event-data) #" ")))))

(defmethod handle-event :default
  [event-type event-data])

(defmethod handle-event "!run-stage"
  [event-type {:keys [content channel-id]}]
  (when (= channel-id discord-admin-channel-id)
    (let [[tournament-name stage-name] (str/split (str/replace content #"!run-stage " "") #" ")]
      (println "Received run")
      (if (:stage-running @state)
        (dmess/create-message! (:messaging @state) channel-id
                               :content "A stage is already running.")
        (do
          (swap! state assoc :stage-running true)
          (run-stage tournament-name stage-name))))))

(defmethod handle-event "!stop-stage"
  [event-type {:keys [channel-id]}]
  (when (= channel-id discord-admin-channel-id)
    (println "Received stop")
    (swap! state assoc :stage-running false)))

(defmethod handle-event "!report"
  [event-type {{username :username id :id disc :discriminator} :author}]
  (let [team (get-team-of-discord-user (str username "#" disc))
        match-ids (ebot/get-match-id-with-team team)
        games-awaiting-close (:games-awaiting-close @state)
        chan (some #(get games-awaiting-close (str (int %))) match-ids)]
    (println team)
    (println match-ids)
    (println games-awaiting-close)
    (println chan)
    ;;todo add nil case for chan
    (async/go
      (async/>! chan "some-data"))))

(defmethod handle-event "!ban"
  [event-type {{username :username id :id disc :discriminator} :author, :keys [channel-id content]}]
  (let [input-rest (rest (str/split content #" "))
        map-name (first input-rest)
        veto-lobby (db/get-veto-lobby channel-id)
        tournament-id (:tournament_id veto-lobby)
        team1 (toornament/participant tournament-id (:team1_id veto-lobby))
        team2 (toornament/participant tournament-id (:team2_id veto-lobby))
        discord-usernames [(get-team-discord-usernames team1)
                           (get-team-discord-usernames team2)]
        teams {(get team1 "id") 0
               (get team2 "id") 1}
        next-ban-index (get teams (:next_ban_id veto-lobby))
        maps-left (get-maps-left veto-lobby)]
    (cond
      (nil? veto-lobby)
      (dmess/create-message! (:messaging @state) channel-id
                             :content "There is no veto in this channel.")

      (not (contains? (set (apply concat discord-usernames))
                      (str username "#" disc)))
      (dmess/create-message! (:messaging @state) channel-id
                             :content (str (format-discord-mentions [id])
                                           " you are not a member of either team."))

      (not (contains? (set (nth discord-usernames next-ban-index))
                      (str username "#" disc)))
      (dmess/create-message! (:messaging @state) channel-id
                             :content (str (format-discord-mentions [id])
                                           " it is not your team's turn to ban."))

      (not (= (count input-rest) 1))
      (dmess/create-message! (:messaging @state) channel-id
                             :content (str (format-discord-mentions [id])
                                           " Usage: `!ban <mapname>` to ban a map."))

      (not (contains? (set maps-left) map-name))
      (dmess/create-message! (:messaging @state) channel-id
                             :content (str (format-discord-mentions [id])
                                           " the remaining maps are:\n" (str/join "\n" maps-left)))

      :else
      (ban-map veto-lobby map-name team1 team2))))

(defmethod handle-event "!delay"
  [event-type {:keys [channel-id content]}]
  (when (= channel-id discord-admin-channel-id)
    (let [input-rest (rest (str/split content #" "))
          match-id (first input-rest)]
      (if (db/match-delayed? match-id)
        (dmess/create-message! (:messaging @state) channel-id
                                :content (str match-id " is already delayed."))
        (do
          (db/delay-match match-id)
          (dmess/create-message! (:messaging @state) channel-id
                                  :content (str match-id " is now delayed.")))))))

(defmethod handle-event "!resume"
  [event-type {:keys [channel-id content]}]
  (when (= channel-id discord-admin-channel-id)
    (let [input-rest (rest (str/split content #" "))
          match-id (first input-rest)]
      (if (not (db/match-delayed? match-id))
        (dmess/create-message! (:messaging @state) channel-id
                                :content (str match-id " is not delayed."))
        (do
          (db/resume-match match-id)
          (dmess/create-message! (:messaging @state) channel-id
                                  :content (str match-id " is now resumed.")))))))

(defn -main
  [& args]
  (let [event-ch (async/chan 100)
        connection-ch (dconn/connect-bot! discord-token event-ch)
        messaging-ch (dmess/start-connection! discord-token)
        init-state {:connection connection-ch
                    :event event-ch
                    :messaging messaging-ch
                    :stage-running false
                    :discord-user-ids {}
                    :games-awaiting-close {}
                    :close-game-time 60000}]
    (reset! state init-state)
    (devent/message-pump! event-ch handle-event)
    (dmess/stop-connection! messaging-ch)
    (dconn/disconnect-bot! connection-ch)))
