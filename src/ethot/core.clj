(ns ethot.core
  (:require [discljord.connections :as dconn]
            [discljord.messaging :as dmess]
            [discljord.events :as devent]
            [clojure.core.async :as async]
            [clojure.pprint :as pp]
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

(defn notify-discord
  [tournament-id team1-id team2-id server-ip server-pass]
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
                                         "\n" "Check your DM's for server credentials."))
    (doseq [discord-id discord-test-user-ids]
      (let [channel-id (:id @(dmess/create-dm! (:messaging @state) discord-id))]
        (dmess/create-message! (:messaging @state) channel-id
                               :content (str team1-name " vs " team2-name " is now ready!"
                                             "\n" "Server: " server-ip
                                             "\n" "Password: " server-pass))))))

(defn start-tournament
  [name]
  (println "\n;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;")
  (println ";              Step 1: Log into eBot Admin Page              ;")
  (println ";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;\n")

  (ebot/login)
  (pp/pprint (ebot/get-admin-page))

  (println "\n;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;")
  (println ";         Step 2: Get the Tournament from Toornament         ;")
  (println ";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;\n")

  (def tournament (toornament/get-tournament name))
  (def tournament-id (get tournament "id"))
  (pp/pprint tournament)

  (println "\n;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;")
  (println ";    Step 3: Get the Importable Matches from Toornament      ;")
  (println ";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;\n")

  (def matches (toornament/importable-matches tournament-id))
  (pp/pprint matches)

  (println "\n;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;")
  (println ";         Step 4: Get the Games for the First Match          ;")
  (println ";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;\n")

  (def match (first matches))
  (def match-id (get match "id"))
  (def games (toornament/games tournament-id match-id))
  (pp/pprint games)

  (println "\n;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;")
  (println ";               Step 4: Import the First Game                ;")
  (println ";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;\n")

  (def game (first games))
  (def game-id (get game "number"))
  (pp/pprint (ebot/import-game tournament-id match-id game-id))

  (println "\n;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;")
  (println ";            Step 5: Assign the Game to a Server             ;")
  (println ";               TODO: Needs to be Implemented                ;")
  (println ";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;\n\n")

  (println "\n;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;")
  (println ";                   Step 6: Notify Discord                   ;")
  (println ";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;\n")

  (notify-discord tournament-id
                  (get-in match ["opponents" 0 "participant" "id"])
                  (get-in match ["opponents" 1 "participant" "id"])
                  "SERVER-IP" "SERVER-PASS")) ;TODO get from Step 5

(defmulti handle-event
  (fn [event-type event-data]
    (when (and
           (not (:bot (:author event-data)))
           (= (:channel-id event-data) discord-admin-channel-id)
           (= event-type :message-create))
      (first (str/split (:content event-data) #" ")))))

(defmethod handle-event :default
  [event-type event-data])

(defmethod handle-event "!start-tournament"
  [event-type {:keys [content channel-id]}]
  (let [tournament-name (second (str/split content #"!start-tournament "))]
    (start-tournament tournament-name)))

(defn -main
  [& args]
  (let [event-ch (async/chan 100)
        connection-ch (dconn/connect-bot! discord-token event-ch)
        messaging-ch (dmess/start-connection! discord-token)
        init-state {:connection connection-ch
                    :event event-ch
                    :messaging messaging-ch}]
    (reset! state init-state)
    (devent/message-pump! event-ch handle-event)
    (dmess/stop-connection! messaging-ch)
    (dconn/disconnect-bot! connection-ch)))
