(ns scicloj.kindly-render.note.to-hiccup-js-test
  (:require [clojure.test :refer [deftest is testing]]
            [scicloj.kindly-render.note.to-hiccup-js :as to-hiccup-js]))

(def note
  {:value [:div {} [{:title {:text "Echarts Example"}, :tooltip {}, :legend {:data ["sales"]}, :xAxis {:data ["Shirts" "Cardigans" "Chiffons" "Pants" "Heels" "Socks"]}, :yAxis {}, :series [{:name "sales", :type "bar", :data [5 20 36 10 10 20]}]} {:title {:text "Echarts Example"}, :tooltip {}, :legend {:data ["sales"]}, :xAxis {:data ["Shirts" "Cardigans" "Chiffons" "Pants" "Heels" "Socks"]}, :yAxis {}, :series [{:name "sales", :type "bar", :data [5 20 36 10 10 20]}]}]], :meta-kind :kind/hiccup, :kindly/options {:hide-code true}, :kind :kind/hiccup, :advice [[:kind/hiccup {:reason :metadata}] [:kind/vector {:reason :predicate}] [:kind/seq {:reason :predicate}] [:kind/hiccup {:reason :metadata}] [:kind/vector {:reason :predicate}] [:kind/seq {:reason :predicate}]], :deps #{:kind/echarts :kind/hiccup}, :hiccup [:div {} [{:title {:text "Echarts Example"}, :tooltip {}, :legend {:data ["sales"]}, :xAxis {:data ["Shirts" "Cardigans" "Chiffons" "Pants" "Heels" "Socks"]}, :yAxis {}, :series [{:name "sales", :type "bar", :data [5 20 36 10 10 20]}]} [:div {:style {:height "200px", :width "100%"}, :class "kind-echarts"} [:script "\n{\nvar myChart = echarts.init(document.currentScript.parentElement);\nmyChart.setOption({\"title\":{\"text\":\"Echarts Example\"},\"tooltip\":{},\"legend\":{\"data\":[\"sales\"]},\"xAxis\":{\"data\":[\"Shirts\",\"Cardigans\",\"Chiffons\",\"Pants\",\"Heels\",\"Socks\"]},\"yAxis\":{},\"series\":[{\"name\":\"sales\",\"type\":\"bar\",\"data\":[5,20,36,10,10,20]}]});\n};"]]]], :idx 4})
(:hiccup note)
(deftest render-test
  (is (= []
         (:hiccup (to-hiccup-js/render (dissoc note :hiccup))))))

