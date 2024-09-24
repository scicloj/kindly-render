(ns scicloj.kindly-render.note.to-markdown
  (:require [clojure.pprint :as pprint]
            [clojure.string :as str]
            [hiccup.core :as hiccup]
            [scicloj.kindly-render.util :as util]
            [scicloj.kindly-render.note.to-hiccup-js :as to-hiccup-js]
            [scicloj.kindly-render.note.to-hiccup :as to-hiccup]))

(defmulti render* :kind)

(defn render [note]
  (render* (util/derefing-advise note)))

(def ^:dynamic *gfm* false)

(defn html [note]
  ;; TODO: which hiccup to call depends...
  (-> (if *gfm*
        (to-hiccup/render note)
        (to-hiccup-js/render note))
      (hiccup/html)))

(defmethod render* :default [note]
  (html note))

(defmethod render* :kind/hidden [note])

(defmethod render* :kind/code [{:keys [code]}]
  code)

(defmethod render* :kind/md [{:keys [value]}]
  (util/normalize-md value))

(defn divide [xs]
  ;; Calling html here makes everything inside the table HTML
  (str "| " (str/join " | " (map #(html (util/derefing-advise {:value %}))
                                 xs))
       " |"))

(defmethod render* :kind/table [{:keys [value]}]
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

(defmethod render* :kind/pprint [{:keys [value]}]
  (result-pprint value))

;; Don't show vars
(defmethod render* :kind/var [note])
