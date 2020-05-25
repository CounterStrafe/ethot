(ns ethot.core-test
  (:require [clojure.test :refer :all]
            [ethot.core :refer :all]))

(deftest maps-left-test
  (testing "maps-left."
    (let [veto-lobby {:match_id "0"
                      :de_inferno "0"
                      :de_overpass "1"
                      :de_shortnuke "0"
                      :de_train "1"
                      :de_vertigo "0"}
          maps-left (get-maps-left veto-lobby)]
      (is (= maps-left (list "de_inferno" "de_shortnuke" "de_vertigo"))))))
