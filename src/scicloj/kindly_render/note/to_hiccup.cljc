(ns scicloj.kindly-render.note.to-hiccup
  (:require [clojure.pprint :as pprint]
            [scicloj.kindly-render.util :as util]
            [scicloj.kindly-render.from-markdown :as from-markdown]))

(defmulti render* :kind)

(defn render [note]
  (render* (util/derefing-advise note)))

(defmethod render* :default [{:keys [value kind]}]
  (if kind
    [:div
     [:div "Unimplemented: " [:code (pr-str kind)]]
     [:code (pr-str value)]]
    (str value)))

(defmethod render* :kind/hidden [note])

;; Don't show vars
(defmethod render* :kind/var [note])

;; TODO: we might not want this
(defmethod render* :kind/md [{:keys [value]}]
  (from-markdown/hiccup value))

(defn pprint [value]
  [:pre [:code (binding [*print-meta* true]
                 (with-out-str (pprint/pprint value)))]])

(defmethod render* :kind/pprint [{:keys [value]}]
  (pprint value))

(defmethod render* :kind/image [{:keys [value]}]
  (if (string? value)
    [:img {:src value}]
    [:div "Image kind not implemented"]))

(defmethod render* :kind/table [{:keys [value]}]
  (let [{:keys [column-names row-vectors]} value]
    [:table
     [:thead
      (into [:tr]
            (for [header column-names]
              [:th (render* header)]))]
     (into [:tbody]
           (for [row row-vectors]
             (into [:tr]
                   (for [column row]
                     [:td (render* column)]))))]))

;; Data types that can be recursive

(defmethod render* :kind/vector [{:keys [value]}]
  (util/expand-data {:class "kind_vector"} value render))

(defmethod render* :kind/map [{:keys [value]}]
  (util/expand-data {:class "kind_map"} (apply concat value) render))

(defmethod render* :kind/set [{:keys [value]}]
  (util/expand-data {:class "kind_set"} value render))

(defmethod render* :kind/seq [{:keys [value]}]
  (util/expand-data {:class "kind_seq"} value render))

;; Special data type hiccup that needs careful expansion

(defmethod render* :kind/hiccup [{:keys [value]}]
  (util/expand-hiccup value render))
