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

(def state (atom {}))

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
                ebot-match-id (ebot/import-game tournament-id match-id game-number)
                ; This code assumes there are more available servers than games
                ; that can be played at one time. There is no logic for
                ; prioritising games earlier in the bracket.
                server-id (ebot/get-available-server)]
            (ebot/assign-server server-id ebot-match-id)
            (swap! state update :imported-matches conj match-id)
            (notify-discord tournament-id
                            (get-in match ["opponents" 0 "participant" "id"])
                            (get-in match ["opponents" 1 "participant" "id"])
                            (ebot/get-server-creds ebot-match-id))))

        ; TODO exports here


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

(defn -main
  [& args]
  (let [event-ch (async/chan 100)
        connection-ch (dconn/connect-bot! discord-token event-ch)
        messaging-ch (dmess/start-connection! discord-token)
        init-state {:connection connection-ch
                    :event event-ch
                    :messaging messaging-ch
                    :stage-running false
                    ; TODO: Figure out how to remove matches from here while the
                    ; bot is running in case we need to re-import a match.
                    :imported-matches #{}}]
    (reset! state init-state)
    (devent/message-pump! event-ch handle-event)
    (dmess/stop-connection! messaging-ch)
    (dconn/disconnect-bot! connection-ch)))
