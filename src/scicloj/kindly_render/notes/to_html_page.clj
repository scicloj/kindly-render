(ns scicloj.kindly-render.notes.to-html-page
  (:require [hiccup.page :as page]
            [scicloj.kindly-render.note.to-hiccup-js :as to-hiccup-js]
            [scicloj.kindly-render.entry.hiccups :as hiccups]))

(defn page [elements]
  (page/html5
    (cond->
      [:head
       (page/include-css "style.css")
       (apply page/include-js (to-hiccup-js/include-js))]
      (:scittle-reagent @to-hiccup-js/*deps*)
      (conj (to-hiccup-js/scittle '[(require '[reagent.core :as r :refer [atom]]
                                             '[reagent.dom :as dom])])))
    (into [:body] elements)))

(defn render-notebook
  "Creates a markdown file from a notebook"
  [{:keys [notes]}]
  (-> (mapcat hiccups/code-and-value notes)
      (page)))
