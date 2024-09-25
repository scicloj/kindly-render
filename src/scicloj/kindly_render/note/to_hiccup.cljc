(ns scicloj.kindly-render.note.to-hiccup
  (:require [clojure.pprint :as pprint]
            [scicloj.kindly-render.util :as util]
            [scicloj.kindly-render.from-markdown :as from-markdown]))

(defmulti render-advice :kind)

(defn render [note]
  (-> (util/derefing-advise note)
      (render-advice)))

(defmethod render-advice :default [{:keys [value kind]}]
  (if kind
    [:div
     [:div "Unimplemented: " [:code (pr-str kind)]]
     [:code (pr-str value)]]
    (str value)))

(defmethod render-advice :kind/hidden [note])

;; Don't show vars
(defmethod render-advice :kind/var [note])

;; TODO: we might not want this
(defmethod render-advice :kind/md [{:keys [value]}]
  (from-markdown/hiccup value))

(defn pprint [value]
  [:pre [:code (binding [*print-meta* true]
                 (with-out-str (pprint/pprint value)))]])

(defmethod render-advice :kind/pprint [{:keys [value]}]
  (pprint value))

(defmethod render-advice :kind/image [{:keys [value]}]
  (if (string? value)
    [:img {:src value}]
    [:div "Image kind not implemented"]))

(defmethod render-advice :kind/table [{:keys [value]}]
  (let [{:keys [column-names row-vectors]} value]
    [:table
     [:thead
      (into [:tr]
            (for [header column-names]
              [:th (render-advice header)]))]
     (into [:tbody]
           (for [row row-vectors]
             (into [:tr]
                   (for [column row]
                     [:td (render-advice column)]))))]))

;; Data types that can be recursive

(defmethod render-advice :kind/vector [{:keys [value]}]
  (util/render-data-recursively {:class "kind_vector"} value render))

(defmethod render-advice :kind/map [{:keys [value]}]
  (util/render-data-recursively {:class "kind_map"} (apply concat value) render))

(defmethod render-advice :kind/set [{:keys [value]}]
  (util/render-data-recursively {:class "kind_set"} value render))

(defmethod render-advice :kind/seq [{:keys [value]}]
  (util/render-data-recursively {:class "kind_seq"} value render))

;; Special data type hiccup that needs careful expansion

(defmethod render-advice :kind/hiccup [{:keys [value]}]
  (util/render-hiccup-recursively value render))
