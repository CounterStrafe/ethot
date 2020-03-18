(ns ethot.core
  (:require [discljord.connections :as dconn]
            [discljord.messaging :as dmess]
            [discljord.events :as devent]
            [clojure.core.async :as async]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [clj-http.client :as hclient]
            [ethot.ebot :as ebot]
            [ethot.toornament :as toornament])
  (:gen-class))

(def state (atom {}))

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
  (pp/pprint (ebot/import-game tournament-id match-id game-id)))

(defn -main
  [& args]
  (start-tournament "Test"))
