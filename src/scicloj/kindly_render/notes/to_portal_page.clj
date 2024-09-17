(ns scicloj.kindly-render.notes.to-portal-page
  (:require [scicloj.kindly-render.note.to-hiccup-js :as to-hiccup-js]
            [scicloj.kindly-render.notes.to-html-page :as html]))

(defn expr-result [{:keys [code] :as note}]
  ;; TODO: maybe error/stdout to show
  (if (contains? note :value)
    [:div
     ;; code
     [:pre [:code code]]
     ;; value
     (to-hiccup-js/portal note)]
    [:div (:code note)]))

(defn notes-to-html-portal [{:keys [notes]}]
  (-> (mapv expr-result notes)
      (html/page)))
