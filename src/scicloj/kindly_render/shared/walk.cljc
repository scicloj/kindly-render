(ns scicloj.kindly-render.shared.walk
  "Walks nested visualizations, rendering them to hiccup"
  (:require [scicloj.kindly-advice.v1.api :as ka]
            [scicloj.kindly-advice.v1.completion :as kc])
  (:import (clojure.lang IDeref)))

;; TODO: this doesn't seem like the right place for this, maybe move to kindly-advice?
(defn derefing-advise
  "Kind priority is inside out: kinds on the value supersedes kinds on the ref."
  [note]
  (let [note (ka/advise note)
        {:keys [value]} note]
    (if (instance? clojure.lang.IDeref value)
      (let [v @value
            meta-kind (kc/meta-kind v)]
        (-> (assoc note :value v)
            (cond-> meta-kind (assoc note :meta-kind meta-kind))
            (derefing-advise)))
      note)))

(defn render-data-recursively
  "Data kinds like vectors, maps, sets, and seqs are recursively rendered."
  [props vs render-advice]
  (into [:div props]
        (for [v vs]
          [:div {:style {:border  "1px solid grey"
                         :padding "2px"}}
           (-> (derefing-advise {:value v})
               (render-advice))])))

(defn reagent?
  "Reagent components may be requested by symbol: `[my-component 1]`
   or by an inline function: `['(fn [] [:h1 123])]`"
  [tag]
  (or (symbol? tag)
      (and (seq? tag) (= 'fn (first tag)))))

(defn scittle?
  "Scittle code can be written as `[(println 'hello)]`"
  [tag]
  (seq? tag))

(defn visualization
  "Identifies values that have kind annotations,
  indicating they are visualizations.
  Hiccup and tagged vectors are not considered visualizations
  as they are already hiccup.
  Returns a note suitable for rendering."
  [x]
  (let [note (derefing-advise {:value x})
        {:keys [meta-kind]} note]
    ;; meta-kind is explicitly annotated, excludes vector/set/map/seq
    (when (or (and meta-kind (not= :kind/hiccup meta-kind))
              ;; vectors can be visualizations, if not valid hiccup
              (and (vector? x)
                   (let [tag (first x)]
                     (not (or (keyword? tag)
                              (reagent? tag)
                              (scittle? tag))))))
      note)))

(defn render-hiccup-recursively
  "Traverses a hiccup tree, and returns a hiccup tree.
  Kinds encountered get rendered to hiccup,
  making a larger hiccup structure that can be converted to HTML.
  Data kinds like vectors, maps, sets, and seqs are recursively rendered."
  [hiccup render-advice]
  (if-let [note (visualization hiccup)]
    (render-advice note)
    (cond (instance? IDeref hiccup)
          (recur @hiccup render-advice)

          (vector? hiccup)
          (let [[tag & children] hiccup
                c (first children)
                attrs (and (map? c) (not (visualization c)) c)]
            (cond (reagent? tag) (render-advice {:kind :kind/reagent :value hiccup})
                  (scittle? tag) (render-advice {:kind :kind/scittle :value hiccup})
                  :else (if attrs
                          (into [tag attrs] (map #(render-hiccup-recursively % render-advice)) (next children))
                          (into [tag] (map #(render-hiccup-recursively % render-advice)) children))))

          :else
          hiccup)))
