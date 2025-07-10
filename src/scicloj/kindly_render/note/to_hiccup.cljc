(ns scicloj.kindly-render.note.to-hiccup
  (:require [clojure.data.codec.base64 :as b64]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [scicloj.kindly-render.shared.from-markdown :as from-markdown]
            [scicloj.kindly-render.shared.recursives :as recursives]
            [scicloj.kindly-render.shared.util :as util]
            [scicloj.kindly-render.shared.walk :as walk])
  (:import (javax.imageio ImageIO)
           (java.awt.image BufferedImage)))

(defmulti render-advice :kind)

(defn render [note]
  (walk/advise-render-style note render-advice))



(defn in-vector [v]
  (if (sequential? v)
    v
    [v]))

(defn escape [string]
  (-> string
      (str/escape
        {\< "&lt;"
         \> "&gt;"
         \& "&amp;"
         \" "&quot;"
         \' "&apos;"})))


(defn clojure-code-item [{:keys [hiccup-element]}]
  (fn [string-or-strings]
    (let [strings (->> string-or-strings
                       in-vector)]
      {:hiccup (->> strings
                    (map (fn [s]
                           [:pre
                            [hiccup-element
                             (escape s)]]))
                    (into [:div]))})))

(def source-clojure
  (clojure-code-item {:hiccup-element :code.sourceCode.language-clojure.source-clojure.bg-light}))



(defn block [class x]
  ;; TODO: can the class go on pre instead? for more visibility in the dom
  [:pre [:code {:class class} x]])

(defn code-block [x]
  (block "sourceCode language-clojure source-clojure bg-light" x))

(defn blockquote [xs]
  (into [:blockquote] xs))

(defn result-block [x]
  (blockquote [(block "sourceCode language-clojure printed-clojure" x)]))

(defn pprint-block [value]
  (result-block (with-out-str (pprint/pprint value))))

(defn message [s channel]
  (blockquote [[:strong channel] (block nil s)]))

(defmethod render-advice :kind/code [{:as note :keys [value]}]
  (->> (:hiccup (source-clojure value))
       (assoc note :hiccup)))

(defmethod render-advice :kind/hidden [note]
  note)

(defmethod render-advice :kind/md [{:as note :keys [value]}]
  (->> (from-markdown/to-hiccup value)
       (assoc note :hiccup)))

(defmethod render-advice :kind/html [{:as note :keys [value]}]
  ;; TODO: is hiccup/raw well supported or do we need to do something?
  (->> (util/kind-str value)
       (assoc note :hiccup)))

(defmethod render-advice :kind/pprint [{:as note :keys [value]}]
  (->> (pprint-block value)
       (assoc note :hiccup)))


(defn src-encode-image [value]
  (let [out (java.io.ByteArrayOutputStream.)
        value (if (sequential? value)
                (first value)
                value)
        _ (ImageIO/write ^BufferedImage value "png" out)]
    (str "data:image/png;base64,"
         (-> out .toByteArray b64/encode String.))))

(defn src-copy-image [value]
  #_(let [png-path (files/next-file!
                     full-target-path
                     ""
                     value
                     ".png")]
      (do
        (when-not
          (util.image/write! value "png" png-path)
          (throw (ex-message "Failed to save image as PNG.")))
        {:hiccup [:img {:src ...}]})
      (-> png-path
          (str/replace
            (re-pattern (str "^"
                             base-target-path
                             "/"))
            ""))))

(defmethod render-advice :kind/image
  [{:as note :keys [value]}]
  (let [value (if (sequential? value)
                (first value)
                value)]
    (->> (cond
           ;; An image url:
           (:src value)
           [:img value]

           ;; A BufferedImage object:
           (instance? BufferedImage value)
           [:img {:src (if true
                         (src-encode-image value)
                         (src-copy-image value))}])
         (assoc note :hiccup))))

;; Data types that can be recursive

(defmethod render-advice :kind/vector [note]
  (recursives/render-vector note render))

(defmethod render-advice :kind/map [note]
  (recursives/render-map note render))

(defmethod render-advice :kind/set [note]
  (recursives/render-set note render))

(defmethod render-advice :kind/seq [note]
  (recursives/render-seq note render))

;; Special data type hiccup that needs careful expansion

(defmethod render-advice :kind/hiccup [note]
  (recursives/render-hiccup note render))

(defmethod render-advice :kind/table [{:as note :keys [render]}]
  (recursives/render-table note render))

(defmethod render-advice :kind/video [{:keys [value] :as note}]
  (let [{:keys [src
                youtube-id
                iframe-width
                iframe-height
                allowfullscreen
                embed-options]
         :or   {allowfullscreen true}}
        value]
    (->> (cond
           ;; A video file
           src [:video {:controls ""}
                [:source {:src  src
                          :type "video/mp4"}]]
           ;; A YouTube video
           youtube-id [:iframe
                       (cond-> {:src             (str "https://www.youtube.com/embed/" youtube-id
                                                      (some->> embed-options
                                                               (map (fn [[k v]]
                                                                      (format "%s=%s" (name k) v)))
                                                               (str/join "&")
                                                               (str "?")))
                                :allowfullscreen allowfullscreen}
                               iframe-height (assoc :height iframe-height)
                               iframe-width (assoc :width iframe-width))])
         (assoc note :hiccup))))

#?(:clj
   (defmethod render-advice :kind/dataset [{:as note :keys [value kindly/options]}]
     (let [{:keys [dataset/print-range]} options]
       (-> value
           (cond-> print-range ((resolve 'tech.v3.dataset.print/print-range) print-range))
           (println)
           (with-out-str)
           (from-markdown/to-hiccup)
           (->> (assoc note :hiccup))))))

(defmethod render-advice :kind/tex [{:as note :keys [value]}]
  (->> (if (vector? value) value [value])
       (map (partial format "$$%s$$"))
       (str/join \newline)
       (from-markdown/to-hiccup)
       (assoc note :hiccup)))

(defmethod render-advice :kind/var [{:keys [value form] :as note}]
  (let [sym (second value)
        s (str "#'" (str *ns*) "/" sym)]
    (assoc note
      :hiccup s)))

(defmethod render-advice :kind/fn [note]
  (recursives/render-kind-fn note render))

(defmethod render-advice :kind/fragment [note]
  (walk/render-fragment-recursively note render))

(defmethod render-advice :default [{:as note :keys [value kind]}]
  (->> (if kind
         [:div
          [:div "Unimplemented: " [:code (pr-str kind)]]
          [:code (pr-str value)]]
         (pprint-block value))
       (assoc note :hiccup)))
