(ns scicloj.kindly-render.util
  (:require [scicloj.kindly-advice.v1.api :as ka]
            [scicloj.kindly-advice.v1.completion :as kc]))

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

(defn kind-request [x]
  (let [note (derefing-advise {:value x})]
    (when (:kind-meta note)
      note)))

(defn reagent?
  "Reagent components may be requested by symbol: `[my-component 1]`
   or by an inline function: `['(fn [] [:h1 123])]`"
  [tag]
  (or (symbol? tag)
      (and (seq? tag)
           (= 'fn (first tag)))))

(defn expand-hiccup [hiccup expander]
  (if-let [note (kind-request hiccup)]
    (expander note)
    (cond (instance? IDeref hiccup)
          (recur @hiccup)

          (vector? hiccup)
          (let [[tag & children] hiccup
                c (first children)
                attrs (and (map? c) (not (kind-request c)) c)]
            (cond (reagent? tag) (expander {:kind :kind/reagent :value hiccup})
                  (seq? tag) (expander {:kind :kind/scittle :value hiccup})
                  :else (if attrs
                          (into [tag attrs] (map expand-hiccup) (next children))
                          (into [tag] (map expand-hiccup) children))))

          :else
          hiccup)))

;; TODO: shouldn't need this (something upstream should have unwrapped it already)
(defn normalize-md [value]
  (if (vector? value)
    (str (first value))
    (str value)))
