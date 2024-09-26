(ns scicloj.kindly-render.notes.to-html-page
  (:require [hiccup.page :as page]
            [scicloj.kindly-render.note.to-hiccup-js :as to-hiccup-js]))

(defn expr-result [{:keys [code] :as note}]
  [:div
   ;; code
   [:pre [:code code]]
   ;; value
   (to-hiccup-js/render note)])

(defn page [elements]
  (page/html5
    [:head
     (page/include-css "style.css")
     (apply page/include-js (to-hiccup-js/include-js))
     (to-hiccup-js/scittle '[(require '[reagent.core :as r :refer [atom]]
                                      '[reagent.dom :as dom])])]
    (into [:body] elements)))

(defn render-notebook
  "Creates a markdown file from a notebook"
  [{:keys [notes]}]
  (-> (mapv expr-result notes)
      (page)))
