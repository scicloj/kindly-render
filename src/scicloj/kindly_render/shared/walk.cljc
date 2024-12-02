(ns scicloj.kindly-render.shared.walk
  "Walks nested visualizations, rendering them to hiccup"
  (:require [scicloj.kindly-advice.v1.api :as ka]
            [scicloj.kindly-advice.v1.completion :as kc])
  (:import (clojure.lang IDeref)))

(def ^:dynamic *js*
  "Use Javascript for visualizations"
  true)

(def ^:dynamic *deps*
  "Remember what nested kinds were encountered"
  (atom #{}))

;; TODO: maybe move deref logic to kindly-advice?
(defn derefing-advise*
  "Kind priority is inside out: kinds on the value supersedes kinds on the ref."
  [note]
  (let [note (ka/advise note)
        {:keys [value]} note]
    (if (instance? clojure.lang.IDeref value)
      (let [v @value
            meta-kind (kc/meta-kind v)]
        (-> (assoc note :value v)
            (cond-> meta-kind (assoc note :meta-kind meta-kind))
            (derefing-advise*)))
      note)))

(defn note-deps
  "Deps come from the kind, and possibly options"
  [note]
  (let [{:keys [kind kindly/options]} note
        {:keys [deps]} options]
    (cond-> #{}
            kind (conj (keyword (name kind)))
            (map? deps) (conj deps)
            (sequential? deps) (into deps)
            (keyword? deps) (conj deps))))

(defn derefing-advise
  "When we discover deps, record them for later use.
  This is done mutably while traversing notes because deps may occur in nested kinds."
  [note]
  (doto (derefing-advise* note)
    (some->> (note-deps)
             (swap! *deps* into))))

(defn render-data-recursively
  "Data kinds like vectors, maps, sets, and seqs are recursively rendered."
  [props vs render]
  (into [:div props]
        (for [v vs]
          [:div {:style {:border  "1px solid grey"
                         :padding "2px"}}
           (render {:value v})])))

(defn reagent?
  "Reagent components may be requested by symbol: `[my-component 1]`
   or by an inline function: `'[(fn [] [:h1 123])]`"
  [tag]
  (or (symbol? tag)
      (and (seq? tag) (= 'fn (first tag)))))

(defn scittle?
  "Scittle code can be written as `'[(println hello)]`"
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
  [hiccup render]
  (if-let [note (visualization hiccup)]
    (render note)
    (cond (instance? IDeref hiccup)
          (recur @hiccup render)

          (vector? hiccup)
          (let [[tag & children] hiccup
                c (first children)
                attrs (and (map? c) (not (visualization c)) c)]
            (cond (reagent? tag) (render {:kind :kind/reagent :value hiccup})
                  (scittle? tag) (render {:kind :kind/scittle :value hiccup})
                  :else (if attrs
                          (into [tag attrs] (map #(render-hiccup-recursively % render)) (next children))
                          (into [tag] (map #(render-hiccup-recursively % render)) children))))

          :else
          hiccup)))

(defn render-table-recursively
  [value render]
  (let [{:keys [column-names row-vectors]} value]
    [:table
     [:thead
      (into [:tr]
            (for [header column-names]
              [:th (render {:value header})]))]
     (into [:tbody]
           (for [row row-vectors]
             (into [:tr]
                   (for [column row]
                     [:td (render {:value column})]))))]))
