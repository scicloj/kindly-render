(ns scicloj.kindly-render.note.to-hiccup-js-test
  (:require [clojure.test :refer [deftest is testing]]
            [scicloj.kindly-render.note.to-hiccup-js :as to-hiccup-js]
            [scicloj.kindly.v4.kind :as kind]
            [scicloj.kindly-render.shared.util :as util]))

(deftest render-test
  (is (= "vegaEmbed(document.currentScript.parentElement, {});"
         (-> (to-hiccup-js/render {:value (kind/vega-lite {})})
             :hiccup
             (util/multi-nth [2 1])))))

