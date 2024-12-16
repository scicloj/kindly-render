(ns scicloj.kindly-render.shared.walk-test
  (:require [clojure.test :refer [deftest is testing]]
            [scicloj.kindly-render.shared.walk :as walk]
            [scicloj.kindly-render.note.to-hiccup-js :as to-hiccup-js]
            [scicloj.kindly.v4.kind :as kind]))

(deftest unions-test
  (is (= #{:a :b :c}
         (walk/unions [:a :b]
                      #{:b :c :a}
                      [:a]))))

(deftest union-into-test
  (is (= #{:a :b :c :d}
         (walk/union-into #{:a}
                          [#{:b}]
                          [#{:c :d}]))))

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
  {:value          [:div {} [{:title {:text "Echarts Example"}, :tooltip {}, :legend {:data ["sales"]}, :xAxis {:data ["Shirts" "Cardigans" "Chiffons" "Pants" "Heels" "Socks"]}, :yAxis {}, :series [{:name "sales", :type "bar", :data [5 20 36 10 10 20]}]} {:title {:text "Echarts Example"}, :tooltip {}, :legend {:data ["sales"]}, :xAxis {:data ["Shirts" "Cardigans" "Chiffons" "Pants" "Heels" "Socks"]}, :yAxis {}, :series [{:name "sales", :type "bar", :data [5 20 36 10 10 20]}]}]]
   ;;:meta-kind      :kind/hiccup
   ;;:kindly/options {:hide-code true}
   ;;:kind           :kind/hiccup
   :deps           #{:kind/echarts :kind/hiccup}
   ;;   :hiccup         [:div {} [{:title {:text "Echarts Example"}, :tooltip {}, :legend {:data ["sales"]}, :xAxis {:data ["Shirts" "Cardigans" "Chiffons" "Pants" "Heels" "Socks"]}, :yAxis {}, :series [{:name "sales", :type "bar", :data [5 20 36 10 10 20]}]} [:div {:style {:height "200px", :width "100%"}, :class "kind-echarts"} [:script "\n{\nvar myChart = echarts.init(document.currentScript.parentElement);\nmyChart.setOption({\"title\":{\"text\":\"Echarts Example\"},\"tooltip\":{},\"legend\":{\"data\":[\"sales\"]},\"xAxis\":{\"data\":[\"Shirts\",\"Cardigans\",\"Chiffons\",\"Pants\",\"Heels\",\"Socks\"]},\"yAxis\":{},\"series\":[{\"name\":\"sales\",\"type\":\"bar\",\"data\":[5,20,36,10,10,20]}]});\n};"]]]]
   :idx            4})

(deftest render-hiccup-recursively-test
  (is (= [] (:hiccup (walk/render-hiccup-recursively {:kind  :kind/hiccup
                                                      :value [:div {} [chart chart]]} to-hiccup-js/render))))
  (is (= [] (:hiccup (walk/render-hiccup-recursively note to-hiccup-js/render))))
  )
