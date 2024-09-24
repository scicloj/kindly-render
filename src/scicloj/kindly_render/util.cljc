(ns scicloj.kindly-render.util
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
        (ka/advise (derefing-advise (cond-> (assoc note :value v)
                                            meta-kind (assoc note :meta-kind meta-kind)))))
      note)))

(defn renderable? [x]
  (let [note (derefing-advise {:value x})
        kind (:kind note)]
    (when (and kind
               (not= :kind/hiccup kind)
               (or (not= :kind/vector kind)
                   (not (keyword? (first x)))))
      note)))

(defn reagent?
  "Reagent components may be requested by symbol: `[my-component 1]`
   or by an inline function: `['(fn [] [:h1 123])]`"
  [tag]
  (or (symbol? tag)
      (and (seq? tag)
           (= 'fn (first tag)))))

(defn expand-data
  "Data kinds like vectors, maps, sets, and seqs are recursively rendered."
  [props vs render]
  (into [:div props]
        (for [v vs]
          [:div {:style {:border  "1px solid grey"
                         :padding "2px"}}
           (render (derefing-advise {:value v}))])))

(defn expand-hiccup
  "Traverses a hiccup tree, and returns a hiccup tree.
  Kinds encountered get rendered to hiccup,
  making a larger hiccup structure that can be converted to HTML.
  Data kinds like vectors, maps, sets, and seqs are recursively rendered."
  [hiccup render]
  (if-let [note (renderable? hiccup)]
    (render note)
    (cond (instance? IDeref hiccup)
          (recur @hiccup render)

          (vector? hiccup)
          (let [[tag & children] hiccup
                c (first children)
                attrs (and (map? c) (not (renderable? c)) c)]
            (cond (reagent? tag) (render {:kind :kind/reagent :value hiccup})
                  (seq? tag) (render {:kind :kind/scittle :value hiccup})
                  :else (if attrs
                          (into [tag attrs] (map #(expand-hiccup % render)) (next children))
                          (into [tag] (map #(expand-hiccup % render)) children))))

          :else
          hiccup)))

;; TODO: shouldn't need this (something upstream should have unwrapped it already)
(defn normalize-md [value]
  (if (vector? value)
    (str (first value))
    (str value)))
