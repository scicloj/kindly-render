(ns scicloj.kindly-render.entry.hiccups-test
  (:require [clojure.test :refer [deftest is testing]]
            [scicloj.kindly-render.entry.hiccups :as hiccups]
            [scicloj.kindly-render.shared.walk :as walk]
            [scicloj.kindly.v4.kind :as kind]))

(def chart
  (kind/echarts {:title   {:text "Echarts Example"}
                 :tooltip {}
                 :legend  {:data ["sales"]}
                 :xAxis   {:data ["Shirts", "Cardigans", "Chiffons", "Pants", "Heels", "Socks"]}
                 :yAxis   {}
                 :series  [{:name "sales"
                            :type "bar"
                            :data [5 20 36 10 10 20]}]}
                {:style {:height "200px"}}))

(def note
  {:value chart})

(deftest note-hiccups-test
  (is (vector? (-> (walk/advise-with-deps note)
                   (hiccups/note-hiccups)
                   :hiccup))))
