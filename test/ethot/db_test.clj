(ns ethot.db-test
  (:require [clojure.test :refer :all]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [ethot.db :refer :all]))

(deftest veto-lobby-test
    (let [match-id "1234567890123456789"
          tournament-id "1234567890123456789"
          ebot-match-id 1337
          team1 {"id" "1234567890123456789"}
          team2 {"id" "1234567890123456780"}
          team1-id (get team1 "id")
          team2-id (get team2 "id")
          discord-channel-id "123456789012345678"]
      (testing "Tear Down."
        (jdbc/execute-one! ds ["delete from veto where
                                match_id = ?" match-id]
                           {:builder-fn rs/as-unqualified-lower-maps}))
      (testing "create-veto-lobby."
        (let [next-ban (create-veto-lobby match-id tournament-id ebot-match-id team1 team2 discord-channel-id)
              veto-lobby (get-veto-lobby discord-channel-id)]
          (is (or (= next-ban team1)
                  (= next-ban team2)))
          (is (or (= (:next_ban_id veto-lobby) team1-id)
                  (= (:next_ban_id veto-lobby) team2-id)))
          (is (= (:match_id veto-lobby) match-id))
          (is (= (:tournament_id veto-lobby) tournament-id))
          (is (= (:ebot_match_id veto-lobby) ebot-match-id))
          (is (= (:team1_id veto-lobby) team1-id))
          (is (= (:team2_id veto-lobby) team2-id))
          (is (= (:discord_channel_id veto-lobby) discord-channel-id))
          (is (= (:de_inferno veto-lobby) "0"))
          (is (= (:de_overpass veto-lobby) "0"))
          (is (= (:de_shortnuke veto-lobby) "0"))
          (is (= (:de_train veto-lobby) "0"))
          (is (= (:de_vertigo veto-lobby) "0"))
          (is (= (:active veto-lobby) "1"))))

      (testing "ban-map."
        (let [result1 (ban-map match-id "de_inferno" team1-id)
              veto-lobby1 (get-veto-lobby discord-channel-id)
              result2 (ban-map match-id "de_overpass" team2-id)
              veto-lobby2 (get-veto-lobby discord-channel-id)]
          (is (= (:de_inferno veto-lobby1) "1"))
          (is (= (:de_overpass veto-lobby1) "0"))
          (is (= (:de_shortnuke veto-lobby1) "0"))
          (is (= (:de_train veto-lobby1) "0"))
          (is (= (:de_vertigo veto-lobby1) "0"))
          (is (= (:de_vertigo veto-lobby1) "0"))

          (is (= (:de_inferno veto-lobby2) "1"))
          (is (= (:de_overpass veto-lobby2) "1"))
          (is (= (:de_shortnuke veto-lobby2) "0"))
          (is (= (:de_train veto-lobby2) "0"))
          (is (= (:de_vertigo veto-lobby2) "0"))
          (is (= (:de_vertigo veto-lobby2) "0"))

          (is (= (:next_ban_id veto-lobby1) team1-id))
          (is (= (:next_ban_id veto-lobby2) team2-id))))

      (testing "end-veto."
        (let [result (end-veto match-id)
              veto-lobby (get-veto-lobby discord-channel-id)]
          (is (nil? veto-lobby))))

      (testing "Tear Down."
        (jdbc/execute-one! ds ["delete from veto where
                                match_id = ?" match-id]
                           {:builder-fn rs/as-unqualified-lower-maps}))))

(deftest no-veto-lobby-test
  (testing "get-veto-lobby nil."
    (is (nil? (get-veto-lobby "123")))))
