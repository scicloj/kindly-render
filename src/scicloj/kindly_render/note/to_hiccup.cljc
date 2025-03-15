(ns scicloj.kindly-render.note.to-hiccup
  (:require
   [clojure.data.codec.base64 :as b64]
   [clojure.pprint :as pprint]
   [clojure.string :as str]
   [scicloj.kindly-render.shared.from-markdown :as from-markdown]
   [scicloj.kindly-render.shared.recursives :as recursives]
   [scicloj.kindly-render.shared.util :as util]
   [scicloj.kindly-render.shared.walk :as walk])
  (:import
   [javax.imageio ImageIO]) 
  )

(defmulti render-advice :kind)

(defn render [note]
  (walk/advise-render-style note render-advice))

(defmethod render-advice :default [{:as note :keys [value kind]}]
  (->> (if kind
         [:div
          [:div "Unimplemented: " [:code (pr-str kind)]]
          [:code (pr-str value)]]
         (str value))
       (assoc note :hiccup)))

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


(defn clojure-code-item [{:keys [tag hiccup-element md-class]}]
  (fn [string-or-strings]
    (let [strings (->> string-or-strings
                       in-vector
                       ;; (map escape)
                       )]
      {tag true
       :hiccup (->> strings
                    (map (fn [s]
                           [:pre
                            [hiccup-element
                             (escape s)]]))
                    (into [:div]))
       :md (->> strings
                (map (fn [s]
                       (format "
::: {.%s}
```clojure
%s
```
:::
" (name md-class) s)))
                (str/join "\n"))})))

(def source-clojure
  (clojure-code-item {:tag :source-clojure
                      :hiccup-element :code.sourceCode.language-clojure.source-clojure.bg-light
                      :md-class :sourceClojure}))


(source-clojure "(defn f [x] (+ x 9))")

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

(defmethod render-advice :kind/image
  [{:as note :keys [value]}]
  (let [out (io/java.io.ByteArrayOutputStream.)
        v
        (if (sequential? value)
          (first value)
          value)
        _ (ImageIO/write v "png" out)
        hiccup [:img {:src (str "data:image/png;base64,"
                                (-> out .toByteArray b64/encode String.))}]]

    (assoc note
           :hiccup hiccup)))


;; TODO: this is problematic because it creates files
#_(defmethod render-advice :kind/image [{:keys [value]}]
    (let [image (if (sequential? value)
                  (first value)
                  value)
          png-path (files/next-file!
                     full-target-path
                     ""
                     image
                     ".png")]
      (when-not
        (util.image/write! image "png" png-path)
        (throw (ex-message "Failed to save image as PNG.")))
      [:img {:src (-> png-path
                      (str/replace
                        (re-pattern (str "^"
                                         base-target-path
                                         "/"))
                        ""))}]))


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
         :or {allowfullscreen true}}
        value]
  
    (merge note 
           (cond
    ;; A video file
             src {:hiccup [:video {:controls ""}
                           [:source {:src src
                                     :type "video/mp4"}]]}
    ;; A youtube video
             youtube-id {:hiccup [:iframe
                                  (merge
                                   (when iframe-height
                                     {:height iframe-height})
                                   (when iframe-width
                                     {:width iframe-width})
                                   {:src (str "https://www.youtube.com/embed/"
                                              youtube-id
                                              (some->> embed-options
                                                       (map (fn [[k v]]
                                                              (format "%s=%s" (name k) v)))
                                                       (str/join "&")
                                                       (str "?")))
                                    :allowfullscreen allowfullscreen})]}))
    ))

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
  (def value value)
  (let [sym (second value)
        s (str "#'" (str *ns*) "/" sym)]
    (assoc note
           :hiccup s)))

(defmethod render-advice :kind/fn [note]
  (recursives/render-kind-fn note render))

