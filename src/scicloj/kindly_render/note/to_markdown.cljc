(ns scicloj.kindly-render.note.to-markdown
  (:require [clojure.pprint :as pprint]
            [clojure.string :as str]
            [hiccup.core :as hiccup]
            [scicloj.kindly-render.util :as util]
            [scicloj.kindly-render.note.to-hiccup-js :as to-hiccup-js]
            [scicloj.kindly-render.note.to-hiccup :as to-hiccup]))

(defmulti markdown :kind)

(def ^:dynamic *gfm* false)

(defn html [note]
  ;; TODO: which hiccup to call depends...
  (-> (if *gfm*
        (to-hiccup/hiccup note)
        (to-hiccup-js/hiccup-js note))
      (hiccup/html)))

(defmethod markdown :default [note]
  (html note))

(defmethod markdown :kind/hidden [note])

(defmethod markdown :kind/code [{:keys [code]}]
  code)

(defmethod markdown :kind/md [{:keys [value]}]
  (util/normalize-md value))

(defn divide [xs]
  ;; Calling html here makes everything inside the table HTML
  (str "| " (str/join " | " (map #(html (util/derefing-advise {:value %}))
                                 xs))
       " |"))

(defmethod markdown :kind/table [{:keys [value]}]
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
  (-> (if *gfm*
        (block value "clojure")
        (block value "clojure {.printedClojure}"))
      (block-quote)))

(defn result-pprint [value]
  (result-block (binding [*print-meta* true]
                  (with-out-str (pprint/pprint value)))))

(defmethod markdown :kind/pprint [{:keys [value]}]
  (result-pprint value))

;; Don't show vars
(defmethod markdown :kind/var [note])

(comment
  ;; leaving collections to html to allow nesting consistently

  (defn adapt-value [note]
    (let [{:keys [value]} note]
      (if (nested-kinds? value)
        (html note)
        (result-pprint value))))

  (defmethod markdown :kind/vec [note]
    (adapt-value note))

  (defmethod markdown :kind/seq [{:keys [value]}]
    (result-pprint value))

  (defmethod markdown :kind/set [{:keys [value]}]
    (result-pprint value))

  (defmethod markdown :kind/map [{:keys [value]}]
    (result-pprint value)))
