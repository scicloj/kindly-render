(ns scicloj.kindly-render.shared.walk
  "Walks nested visualizations, rendering them to hiccup"
  (:require [clojure.string :as str]
            [scicloj.kindly.v4.api :as kindly]
            [scicloj.kindly-advice.v1.api :as ka]
            [scicloj.kindly-advice.v1.completion :as kc])
  (:import (clojure.lang IDeref)))

(defn show?
  "note must have been advised to determine if it should be shown"
  [note]
  (and (contains? note :value)
       (not (get-in note [:kindly/options :hide-value]))))

(defn js? [{:keys [kindly/options]}]
  (:js options true))

;; TODO: maybe move deref logic to kindly-advice?
(defn derefing-advise
  "Kind priority is inside out: kinds on the value supersedes kinds on the ref."
  [note]
  (let [note (ka/advise note)
        {:keys [value]} note]
    (if (instance? IDeref value)
      (let [v @value
            meta-kind (kc/meta-kind v)]
        (-> (assoc note :value v)
            (cond-> meta-kind (assoc note :meta-kind meta-kind))
            (derefing-advise)))
      note)))

(defn optional-deps
  "Finds deps from kindly/options"
  [{:keys [kindly/options]}]
  (let [{:keys [deps]} options]
    (cond (set? deps) deps
          (map? deps) #{deps}
          (sequential? deps) (set deps)
          (keyword? deps) #{deps})))

(defn unions
  "Like set/union, but handles sequences or sets"
  [& xs]
  (reduce into #{} xs))

(defn union-into
  "Like unions, but extra arguments are sequences of sets or sequences to put into the first"
  [x & xs]
  (apply unions x (apply concat xs)))

(defn note-deps
  "Deps come from the kind, and possibly options"
  [{:as note :keys [kind]}]
  (update note :deps unions
          (when kind #{kind})
          (optional-deps note)))

(defn advise-with-deps
  "Updates advice and deps of a note"
  [note]
  (-> (derefing-advise note)
      (note-deps)))

(defn render-data-recursively
  "Data kinds like vectors, maps, sets, and seqs are recursively rendered.
  There may be nested kinds to render, and possibly deps discovered."
  [note props vs render]
  (let [notes (for [v vs]
                (render {:value v}))]
    (-> note
        (update :deps union-into (keep :deps notes))
        (assoc :hiccup (into [:div props]
                             (for [{:keys [hiccup]} notes]
                               [:div {:style {:border  "1px solid grey"
                                              :padding "2px"}}
                                hiccup]))))))

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

(defn attrs?
  "Detect a plain map to be used as attrs, not a visualization"
  [x]
  (and (map? x)
       (let [{:keys [meta-kind kind]} (ka/advise {:value x})]
         (and (= kind :kind/map)
              ;; meta kinds were explicitly annotated for visualization
              (not meta-kind)))))

(defn render-hiccup-recursively
  "Traverses a hiccup tree, and returns a hiccup tree.
  Kinds encountered get rendered to hiccup,
  making a larger hiccup structure that can be converted to HTML.
  Data kinds like vectors, maps, sets, and seqs are recursively rendered."
  [{:as note :keys [kind value]} render]
  (cond
    ;; non-hiccup child that should be rendered
    (not (vector? value))
    (if (= kind :kind/hiccup)
      (throw (ex-info "kind/hiccup is only meaningful on vectors"
                      {:id   ::kind-hiccup-non-vector
                       :note note}))
      (render note))

    ;;;; below here value must be a vector

    ;; annotated as something other than hiccup
    (and kind (not= kind :kind/hiccup))
    (render note)

    ;; special syntax for reagent
    (reagent? (first value))
    (render {:kind :kind/reagent :value value})

    ;; special syntax for scittle
    (scittle? (first value))
    (render {:kind :kind/scittle :value value})

    ;; vector that is not hiccup
    (not (keyword? (first value)))
    (render note)

    :else-hiccup
    (let [[tag & children] value
          c (first children)
          attrs (and (attrs? c) c)
          children (if attrs
                     (next children)
                     children)
          notes (for [child children]
                  (render-hiccup-recursively {:value child} render))]
      (-> (update note :deps union-into (keep :deps notes))
          (assoc :hiccup (into (if attrs
                                 [tag attrs]
                                 [tag])
                               (map :hiccup notes)))))))

(defn- table-info-from-keys [column-names row-vectors row-maps]

  {:column-names (or column-names (keys (first row-maps)))
   :row-vectors (or row-vectors (map vals row-maps))})

(defn map-of-vectors-to-vector-of-maps [m]
  (let [keys (keys m)
        vals (apply map vector (vals m))]
    (map #(zipmap keys %) vals)))


(defn- table-info-from-value [value]

  (def value value)  
  (cond 
    (map? value)
    {:column-names (keys value)
     :row-vectors (map vals (map-of-vectors-to-vector-of-maps value))}
    (map? (first value))
    {:column-names (keys (first value))
     :row-vectors (map vals value)}
    (sequential? (first value))    
    {:column-names []
     :row-vectors value}))

(defn render-table-recursively
  [{:as note :keys [value]} render]
  
  (let [{:keys [column-names row-vectors row-maps]} value
        
        table-info
        (if (and 
             (nil? column-names)
             (nil? row-vectors)
             (nil? row-maps))
          
          (table-info-from-value value)
          (table-info-from-keys column-names row-vectors row-maps))
        _ (def table-info table-info)
        
        header-notes (for [column-name (:column-names table-info)]
                       (render {:value column-name}))
        row-notes (for [row (:row-vectors table-info)]
                    (for [column row]
                      (render {:value column})))]
    (-> note
        (update :deps union-into
                (keep :deps header-notes)
                (keep :deps (apply concat row-notes)))
        (assoc :hiccup [:table
                        [:thead (into [:tr]
                                      (for [header header-notes]
                                        (:hiccup header)))]
                        (into [:tbody]
                              (for [row row-notes]
                                (into [:tr]
                                      (for [column row]
                                        [:td (:hiccup column)]))))]))))

(defmacro condp->
  "Takes an expression and a set of predicate/form pairs. Threads expr (via ->)
  through each form for which the corresponding predicate is true of expr.
  Note that, unlike cond branching, condp-> threading does not short circuit
  after the first true test expression."
  [expr & clauses]
  (assert (even? (count clauses)))
  (let [g (gensym)
        pstep (fn [[pred step]] `(if (~pred ~g) (-> ~g ~step) ~g))]
    `(let [~g ~expr
           ~@(interleave (repeat g) (map pstep (partition 2 clauses)))]
       ~g)))

(defn kind-class [kind]
  (when (not= kind :kind/hiccup)
    (-> (str (symbol kind))
        (str/replace "/" "-"))))

(defn join-classes [classes]
  (some->> (remove nil? classes)
           (mapcat #(str/split % #"\s+"))
           (distinct)
           (seq)
           (str/join " ")))

(defn kindly-style [{:as rendered-note :keys [kind kindly/options hiccup]}]
  (if (and kind (vector? hiccup))
    (->> (let [[tag & more] hiccup
               attrs (first more)
               class (join-classes [(kind-class kind)
                                    (:class options)
                                    (when (map? attrs)
                                      (:class attrs))])
               m (cond-> (select-keys options [:style])
                         (not (str/blank? class)) (assoc :class class))]
           (if (map? attrs)
             (update hiccup 1 kindly/deep-merge m)
             (into [tag m] more)))
         (assoc rendered-note :hiccup))
    ;; else - no kind
    rendered-note))

(defn advise-render-style [note render]
  (condp-> (advise-with-deps note)
           show? (-> (render)
                     (kindly-style))))

(defn index-notes [notes]
  (map-indexed (fn [idx x] (assoc x :idx idx)) notes))
