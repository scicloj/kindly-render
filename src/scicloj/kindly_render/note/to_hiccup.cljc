(ns scicloj.kindly-render.note.to-hiccup
  (:require [clojure.pprint :as pprint]
            [clojure.string :as str]
            [scicloj.kindly.v4.api :as kindly]
            [scicloj.kindly-render.shared.walk :as walk]
            [scicloj.kindly-render.shared.util :as util]
            [scicloj.kindly-render.shared.from-markdown :as from-markdown]))

(defmulti render-advice :kind)

(defn extend-class [c kind]
  (if (= kind :kind/hiccup)
    c
    (str c
         (when (some? c) " ")
         (-> (str (symbol kind))
             (str/replace "/" "-")))))

(defn kindly-style [hiccup {:as advice :keys [kind kindly/options]}]
  (if (and kind (seq hiccup))
    (let [m (-> (select-keys options [:class :style])
                (update :class extend-class kind))
          [tag attrs & more] hiccup]
      (if (map? attrs)
        (update hiccup 1 kindly/deep-merge m)
        (into [tag m] more)))
    ;; else - no kind
    hiccup))

(defn render [note]
  (let [advice (walk/derefing-advise note)
        hiccup (render-advice advice)]
    (kindly-style hiccup advice)))

(defmethod render-advice :default [{:keys [value kind]}]
  (if kind
    [:div
     [:div "Unimplemented: " [:code (pr-str kind)]]
     [:code (pr-str value)]]
    (str value)))

(defmethod render-advice :kind/code [{:keys [code]}]
  [:pre [:code code]])

(defmethod render-advice :kind/hidden [note])

;; Don't show vars
(defmethod render-advice :kind/var [note])

(defmethod render-advice :kind/md [{:keys [value]}]
  (from-markdown/to-hiccup value))

(defmethod render-advice :kind/html [{:keys [value]}]
  (util/kind-str value))

(defmethod render-advice :kind/pprint [{:keys [value]}]
  [:pre [:code (binding [*print-meta* true]
                 (with-out-str (pprint/pprint value)))]])

(defmethod render-advice :kind/image [{:keys [value]}]
  (if (string? value)
    [:img {:src value}]
    [:div "Image kind not implemented"]))

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

(defmethod render-advice :kind/vector [{:keys [value]}]
  (walk/render-data-recursively {:class "kind_vector"} value render))

(defmethod render-advice :kind/map [{:keys [value]}]
  (walk/render-data-recursively {:class "kind_map"} (apply concat value) render))

(defmethod render-advice :kind/set [{:keys [value]}]
  (walk/render-data-recursively {:class "kind_set"} value render))

(defmethod render-advice :kind/seq [{:keys [value]}]
  (walk/render-data-recursively {:class "kind_seq"} value render))

;; Special data type hiccup that needs careful expansion

(defmethod render-advice :kind/hiccup [{:keys [value]}]
  (walk/render-hiccup-recursively value render))

(defmethod render-advice :kind/table [{:keys [value]}]
  (walk/render-table-recursively value render))

(defmethod render-advice :kind/video [{:keys [youtube-id
                                              iframe-width
                                              iframe-height
                                              allowfullscreen
                                              embed-options]
                                       :or   {allowfullscreen true}}]
  [:iframe
   (merge
     (when iframe-height
       {:height iframe-height})
     (when iframe-width
       {:width iframe-width})
     {:src             (str "https://www.youtube.com/embed/"
                            youtube-id
                            (some->> embed-options
                                     (map (fn [[k v]]
                                            (format "%s=%s" (name k) v)))
                                     (str/join "&")
                                     (str "?")))
      :allowfullscreen allowfullscreen})])

#?(:clj
   (defmethod render-advice :kind/dataset [{:keys [value kindly/options]}]
     (let [{:keys [dataset/print-range]} options]
       (-> value
           (cond-> print-range
                   ((resolve 'tech.v3.dataset.print/print-range) print-range))
           (println)
           (with-out-str)
           (from-markdown/to-hiccup)))))

(defmethod render-advice :kind/tex [{:keys [value]}]
  (->> (if (vector? value) value [value])
       (map (partial format "$$%s$$"))
       (str/join \newline)
       (from-markdown/to-hiccup)))
