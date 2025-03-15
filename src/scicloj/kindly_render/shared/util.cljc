(ns scicloj.kindly-render.shared.util
  (:require [clojure.string :as str]
            [clojure.pprint :as pprint]
            #?(:clj [clojure.data.json :as json])))

(defn json-str [value]
  #?(:clj  (json/write-str value)
     :cljs (js/JSON.stringify (clj->js value))))

(defn kind-str [value]
  (str/join \newline (if (vector? value) value [value])))

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

(defn format-code [form]
  (binding [pprint/*print-pprint-dispatch* pprint/code-dispatch]
    (with-out-str (pprint/pprint form))))

(defn scittle [forms]
  [:script {:type "application/x-scittle"}
   (str/join \newline (map format-code forms))])
