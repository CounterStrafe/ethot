(ns ethot.core
  (:require [discljord.connections :as dconn]
            [discljord.messaging :as dmess]
            [discljord.events :as devent]
            [clojure.core.async :as async]
            [clojure.string :as str]
            [clj-http.client :as hclient]
            [config.core :refer [env]]
            [ethot.ebot :as ebot]
            [ethot.toornament :as toornament])
  (:gen-class))

(def state (atom {:games-awaiting-close {}
                  :close-game-time 120000}))

(def discord-admin-channel-id (:discord-admin-channel-id env))
(def discord-announcements-channel-id (:discord-announcements-channel-id env))
; TODO: Remove once we figure out how we're getting user-ids
(def discord-test-user-ids (:discord-test-user-ids env))
(def discord-token (:discord-token env))

(defn format-discord-mentions
  "Takes a sequence of Discord ID's and retuns a string that mentions them."
  [discord-ids]
  (str/join " " (map #(str "<@" % ">") discord-ids)))

(defn notify-discord
  "Announces the game in the announcements channel and DM's all the players with
  the server creds."
  [tournament-id team1-id team2-id {:keys [ip config_password]}]
  (let [team1 (toornament/participant tournament-id team1-id)
        team2 (toornament/participant tournament-id team2-id)
        team1-name (get team1 "name")
        team2-name (get team2 "name")
        ; Not used now but we will need it when trying to determine who to send
        ; the DM's to.
        discord-usernames (map #(get-in % ["custom_fields" "discord_username"])
                               (concat (get team1 "lineup") (get team2 "lineup")))]
    (dmess/create-message! (:messaging @state) discord-announcements-channel-id
                           :content (str team1-name " vs " team2-name " is now ready!"
                                         "\n" (format-discord-mentions discord-test-user-ids)
                                         "\n" "Check your DM's for server credentials."))
    (doseq [discord-id discord-test-user-ids]
      (let [channel-id (:id @(dmess/create-dm! (:messaging @state) discord-id))]
        (dmess/create-message! (:messaging @state) channel-id
                               :content (str team1-name " vs " team2-name " is now ready!"
                                             "\n" "Server: " ip
                                             "\n" "Password: " config_password))))))

(defn unimported-matches
  "Returns the matches that can and have not been imported yet."
  [tournament-id]
  (filter #(not (contains? (:imported-matches @state) (get % "id")))
          (toornament/importable-matches tournament-id)))

(defn await-game-status
  "waits for the channel to recieve a map of inforamiotn about the caller
  or nil from timeout and will the find the game to delay exporting and make sure"
  [time-to-wait id]
  (let [chan (async/timeout time-to-wait)]
    (async/go
      (if (nil? (async/<! chan))
        (do
          (println (str "exported game: " id))
          (ebot/export-game id)
          (swap! state dissoc id))
        (println "report process")))
    chan))

(defn export-games
  "Will find new games that have recently ended and create a new channel that
  will be notified when either the 5min timeout is reached to allow the next
  game in the bracket to be started OR a player reported suspicious activity
  and will stop the starting of the next game until manually restarted by a TO"
  [state tournament-id stage-id]
  (let [{:keys [games-awaiting-close]} state
        ready-games (toornament/importable-matches tournament-id)
        identifier-ids (map #(str tournament-id
                                  (get % "id")
                                  1) ready-games)
        recently-ended (ebot/get-newly-ended-games identifier-ids)]
    (assoc state :games-awaiting-close
     (merge (zipmap recently-ended
                    (repeatedly await-game-status))
            games-awaiting-close))))

(defn run-stage
  "Logs into eBot and continuously imports and exports all available games
  every 30 seconds."
  [tournament-name stage-name]
  (async/go
    (ebot/login)
    (let [tournament-id (get (toornament/get-tournament tournament-name) "id")
          stage-id (get (toornament/get-stage tournament-id stage-name) "id")]
      (loop []
        (println "Running")
        (doseq [match (unimported-matches tournament-id)]
          (let [match-id (get match "id")
                ; Currently we only support single-game matches
                game (first (toornament/games tournament-id match-id))
                game-number (get game "number")
                ebot-match-id (ebot/import-game tournament-id match-id game-number)]
            (ebot/assign-server 1 ebot-match-id) ; TODO remove hardcoded server-id
            (swap! state update :imported-matches conj match-id)
            (notify-discord tournament-id
                            (get-in match ["opponents" 0 "participant" "id"])
                            (get-in match ["opponents" 1 "participant" "id"])
                            (ebot/get-server-creds ebot-match-id))))

        ;exports here
        (swap! state export-games tournament-id stage-id)
        (async/<! (async/timeout 30000))
        (if (or (not (:stage-running @state))
                (toornament/stage-complete? tournament-id stage-id))
          (println "Stopping")
          (recur))))))

(defmulti handle-event
  (fn [event-type event-data]
    (when (and
           (not (:bot (:author event-data)))
           (= (:channel-id event-data) discord-admin-channel-id)
           (= event-type :message-create))
      (first (str/split (:content event-data) #" ")))))

(defmethod handle-event :default
  [event-type event-data])

(defmethod handle-event "!run-stage"
  [event-type {:keys [content channel-id]}]
  (let [[tournament-name stage-name] (str/split (str/replace content #"!run-stage " "") #" ")]
    (println "Received run")
    (if (:stage-running @state)
      (dmess/create-message! (:messaging @state) channel-id
                             :content "A stage is already running.")
      (do
        (swap! state assoc :stage-running true)
        (run-stage tournament-name stage-name)))))

(defmethod handle-event "!stop-stage"
  [event-type {:keys [content channel-id]}]
  (println "Received stop")
  (swap! state assoc :stage-running false))

(defmethod handle-event "!report"
  [event-type {{username :username id :id disc :discriminator} :author}]
  (println "todo"))

(defn -main
  [& args]
  (let [event-ch (async/chan 100)
        connection-ch (dconn/connect-bot! discord-token event-ch)
        messaging-ch (dmess/start-connection! discord-token)
        init-state {:connection connection-ch
                    :event event-ch
                    :messaging messaging-ch
                    :stage-running false
                    :imported-matches #{}}]
    (reset! state init-state)
    (devent/message-pump! event-ch handle-event)
    (dmess/stop-connection! messaging-ch)
    (dconn/disconnect-bot! connection-ch)))
