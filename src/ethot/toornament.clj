(ns ethot.toornament
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [clj-http.client :as hclient]
            [config.core :refer [env]])
  (:gen-class))

(def toornament-api-key (:toornament-api-key env))
(def toornament-client-id (:toornament-client-id env))
(def toornament-client-secret (:toornament-client-secret env))
(def toornament-url "https://api.toornament.com")

(defn process-response
  "Returns the JSON decoded body of the response."
  [resp]
  (json/read-str (:body resp)))

(defn oauth
  "Returns an access token with the provided scope."
  [scope]
  (let [url (str toornament-url "/oauth/v2/token")]
    (get (process-response
           (hclient/post url {:form-params {:grant_type "client_credentials"
                                            :client_id toornament-client-id
                                            :client_secret toornament-client-secret
                                            :scope (str "organizer:" scope)}}))
      "access_token")))

(defn tournaments
  "Returns all tournaments."
  []
  (let [url (str toornament-url "/organizer/v2/tournaments")]
    (process-response
      (hclient/get url {:headers {:X-Api-Key toornament-api-key
                                  :Authorization (oauth "view")
                                  :Range "tournaments=0-49"}}))))

(defn get-tournament
  "Returns the tournament with the provided name."
  [name]
  (some #(when (= (get % "name") name) %) (tournaments)))

(defn stages
  "Returns all stages in the tournament."
  [tournament-id]
  (let [url (str toornament-url "/organizer/v2/tournaments/" tournament-id "/stages")]
    (process-response
      (hclient/get url {:headers {:X-Api-Key toornament-api-key
                                  :Authorization (oauth "result")}}))))

(defn matches
  "Returns all matches in the tournament."
  [tournament-id]
  (let [url (str toornament-url "/organizer/v2/tournaments/" tournament-id "/matches")]
    (process-response
      (hclient/get url {:headers {:X-Api-Key toornament-api-key
                                  :Authorization (oauth "result")
                                  :Range "matches=0-99"}}))))

(defn importable-matches
  "Returns all matches that are ready to be imported into eBot."
  [tournament-id]
  (filter #(and (= (get % "status") "pending")
                (get-in % ["opponents" 0 "participant"])
                (get-in % ["opponents" 1 "participant"]))
          (matches tournament-id)))

(defn games
  "Returns all games in a match."
  [tournament-id match-id]
  (let [url (str toornament-url "/organizer/v2/tournaments/" tournament-id "/matches/" match-id "/games")]
    (process-response
      (hclient/get url {:headers {:X-Api-Key toornament-api-key
                                  :Authorization (oauth "result")
                                  :Range "games=0-49"}}))))

(defn set-game-status
  "Set the status of the game. Returns the response."
  [tournament-id match-id game-number status]
  (let [url (str toornament-url "/organizer/v2/tournaments" tournament-id "/matches/" match-id "/games/" game-number)]
    (process-response
      (hclient/patch url {:headers {:X-Api-Key toornament-api-key
                                    :Authorization (oauth "result")}
                          :form-params {:status status}}))))

(defn participant
  "Returns the participant."
  [tournament-id participant-id]
  (let [url (str toornament-url "/organizer/v2/tournaments/" tournament-id "/participants/" participant-id)]
    (process-response
      (hclient/get url {:headers {:X-Api-Key toornament-api-key
                                  :Authorization (oauth "participant")}}))))
