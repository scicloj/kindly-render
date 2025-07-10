(ns scicloj.kindly-render.note.to-markdown
  (:require [clojure.pprint :as pprint]
            [clojure.string :as str]
            [hiccup.core :as hiccup]
            [scicloj.kindly-render.shared.walk :as walk]
            [scicloj.kindly-render.shared.util :as util]
            [scicloj.kindly-render.note.to-hiccup-js :as to-hiccup-js]
            [scicloj.kindly-render.note.to-hiccup :as to-hiccup]))

(defmulti render-advice :kind)

(defn render [note]
  (walk/advise-render-style note render-advice))

(defn html [note]
  (-> (if (walk/js? note)
        (to-hiccup-js/render note)
        (to-hiccup/render note))
      (walk/kindly-style)
      (:hiccup)
      (hiccup/html)))

;; fallback to hiccup
;; datastructures and hiccup are left to hiccup for consistency
(defmethod render-advice :default [note]
  (->> (html note)
       (assoc note :md)))

(defmethod render-advice :kind/hidden [note]
  note)

(defmethod render-advice :kind/md [{:as note :keys [value]}]
  (->> (util/kind-str value)
       (assoc note :md)))

(defn divide [xs]
  (str "| "
       ;; Calling html here makes everything inside the table HTML
       (str/join " | " (map #(html {:value %}) xs))
       " |"))

(defmethod render-advice :kind/table [{:as note :keys [value]}]
  (->> (let [{:keys [column-names row-vectors]} value]
         (str (divide column-names) \newline
              (divide (repeat (count column-names) "----")) \newline
              (str/join \newline
                        (for [row row-vectors]
                          (divide row)))))
       (assoc note :md)))

(defn block [s language]
  (str "```" language \newline
       (str/trim-newline s) \newline
       "```"))

(defn blockquote [s]
  (->> (str/split-lines s)
       (map #(str "> " %))
       (str/join \newline)))

(defn message [s channel]
  (-> (str "**" channel "**" \newline \newline
           (block s ""))
      (blockquote)))

(defn code-block [s js]
  (block s (if js
             "clojure {.sourceClojure}"
             "clojure")))

(defmethod render-advice :kind/code [{:as note :keys [code]}]
  (->> (code-block code (walk/js? note))
       (assoc note :md)))

;; There are several potential ways to print values:
;; ```edn
;; ```clojure {.printedClojure}
;; pandoc removes {.printedClojure}
;; ```clojure class=printedClojure
;; >```clojure
;; <pre><code>...</code></pre>

(defn result-block [value js]
  (blockquote (block value (if js
                             "clojure {.printedClojure}"
                             "clojure"))))

(defn result-pprint [value js]
  (result-block (binding [*print-meta* true]
                  (with-out-str (pprint/pprint value)))
                js))

(defmethod render-advice :kind/code [{:as note :keys [code language]}]
  (->> (block language code)
       (assoc note :md)))

(defmethod render-advice :kind/pprint [{:as note :keys [value]}]
  (->> (result-pprint value (walk/js? note))
       (assoc note :md)))

(defmethod render-advice :kind/observable [{:as note :keys [value]}]
  (->> (format "
```{ojs}
//| echo: false
%s
```"
               (util/kind-str value))
       (assoc note :md)))

#?(:clj
   (defmethod render-advice :kind/dataset [{:as note :keys [value kindly/options]}]
     (let [{:keys [dataset/print-range]} options]
       (-> value
           (cond-> print-range ((resolve 'tech.v3.dataset.print/print-range) print-range))
           (println)
           (with-out-str)
           (->> (assoc note :md))))))

(defmethod render-advice :kind/tex [{:as note :keys [value]}]
  (->> (if (vector? value) value [value])
       (map (partial format "$$%s$$"))
       (str/join \newline)
       (assoc note :md)))

(defmethod render-advice :kind/fragment [note]
  (walk/render-fragment-md-recursively note render))
