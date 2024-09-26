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
  (-> (walk/derefing-advise note)
      (render-advice)))

(def ^:dynamic *gfm* false)

(defn html [note]
  ;; TODO: which hiccup to call depends...
  (-> (if *gfm*
        (to-hiccup/render note)
        (to-hiccup-js/render note))
      (hiccup/html)))


;; fallback to hiccup
;; datastructures and hiccup are left to hiccup for consistency
(defmethod render-advice :default [note]
  (html note))

(defmethod render-advice :kind/hidden [note])

(defmethod render-advice :kind/code [{:keys [code]}]
  code)

(defmethod render-advice :kind/md [{:keys [value]}]
  (util/kind-str value))

(defn divide [xs]
  (str "| "
       ;; Calling html here makes everything inside the table HTML
       (str/join " | " (map #(html {:value %}) xs))
       " |"))

(defmethod render-advice :kind/table [{:keys [value]}]
  (let [{:keys [column-names row-vectors]} value]
    (str (divide column-names) \newline
         (divide (repeat (count column-names) "----")) \newline
         (str/join \newline
                   (for [row row-vectors]
                     (divide row))))))

(defn block [s language]
  (str "```" language \newline
       (str/trim-newline s) \newline
       "```"))

(defn block-quote [s]
  (->> (str/split-lines s)
       (map #(str "> " %))
       (str/join \newline)))

(defn message [s channel]
  (-> (str "**" channel "**" \newline \newline
           (block s ""))
      (block-quote)))

;; There are several potential ways to print values:
;; ```edn
;; ```clojure {.printedClojure}
;; pandoc removes {.printedClojure}
;; ```clojure class=printedClojure
;; >```clojure
;; <pre><code>...</code></pre>

(defn result-block [value]
  (block-quote (if *gfm*
                 (block value "clojure")
                 (block value "clojure {.printedClojure}"))))

(defn result-pprint [value]
  (result-block (binding [*print-meta* true]
                  (with-out-str (pprint/pprint value)))))

(defmethod render-advice :kind/pprint [{:keys [value]}]
  (result-pprint value))

;; Don't show vars
(defmethod render-advice :kind/var [note])

(defmethod render-advice :kind/observable [{:keys [value]}]
  (format "
```{ojs}
//| echo: false
%s
```"
          (util/kind-str value)))

#?(:clj
   (defmethod render-advice :kind/dataset [{:keys [value kindly/options]}]
     (let [{:keys [dataset/print-range]} options]
       (-> value
           (cond-> print-range
                   ((resolve 'tech.v3.dataset.print/print-range) print-range))
           (println)
           (with-out-str)))))

(defmethod render-advice :kind/tex [{:keys [value]}]
  (->> (if (vector? value) value [value])
       (map (partial format "$$%s$$"))
       (str/join \newline)))
