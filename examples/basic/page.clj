(ns basic.page
  (:require [scicloj.kindly-advice.v1.api :as kindly-advice]
            [scicloj.kindly-render.notes.to-html-page :as h]
            [scicloj.kindly.v4.kind :as kind]))

(def notebook
  ;; TODO: call advise if there is no advice already later in the chain.
  {:notes [(kindly-advice/advise
             {:value (kind/echarts {:title   {:text "Echarts Example"}
                                    :tooltip {}
                                    :legend  {:data ["sales"]}
                                    :xAxis   {:data ["Shirts", "Cardigans", "Chiffons",
                                                     "Pants", "Heels", "Socks"]}
                                    :yAxis   {}
                                    :series  [{:name "sales"
                                               :type "bar"
                                               :data [5 20 36
                                                      10 10 20]}]})})]})

(spit "basic.html"
      (h/notes-to-html notebook))


;; TODO:
;; 1. Does it work for Clay?
;; 2. Does it work from ClojureScript?

;; TODO: Nested stuff
