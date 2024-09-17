(ns scicloj.kindly-render.note.to-hiccup
  (:require [clojure.pprint :as pprint]
            [scicloj.kindly-render.util :as util]
            [scicloj.kindly-render.from-markdown :as from-markdown])
  (:import (clojure.lang IDeref)))

(defmulti hiccup :kind)

(defmethod hiccup :default [{:keys [value kind]}]
  (if kind
    [:div
     [:div "Unimplemented: " [:code (pr-str kind)]]
     [:code (pr-str value)]]
    (str value)))

(defmethod hiccup :kind/hidden [note])

;; Don't show vars
(defmethod hiccup :kind/var [note])

;; TODO: we might not want this
(defmethod hiccup :kind/md [{:keys [value]}]
  (from-markdown/hiccup value))

(defn pprint [value]
  [:pre [:code (binding [*print-meta* true]
                 (with-out-str (pprint/pprint value)))]])

(defmethod hiccup :kind/pprint [{:keys [value]}]
  (pprint value))

(defn adapt-value [v]
  (hiccup (util/derefing-advise {:value v})))

(defn grid [props vs]
  (into [:div props]
        ;; TODO: adapt outside! not a grid except by css
        (for [v vs]
          [:div {:style {:border  "1px solid grey"
                         :padding "2px"}}
           (adapt-value v)])))

(defmethod hiccup :kind/vector [{:keys [value]}]
  (grid {:class "kind_vector"} value))

(defmethod hiccup :kind/map [{:keys [value]}]
  (grid {:class "kind_map"} (apply concat value)))

(defmethod hiccup :kind/set [{:keys [value]}]
  (grid {:class "kind_set"} value))

(defmethod hiccup :kind/seq [{:keys [value]}]
  (grid {:class "kind_seq"} value))

(defmethod hiccup :kind/image [{:keys [value]}]
  (if (string? value)
    [:img {:src value}]
    [:div "Image kind not implemented"]))

(defmethod hiccup :kind/table [{:keys [value]}]
  (let [{:keys [column-names row-vectors]} value]
    [:table
     [:thead
      (into [:tr]
            (for [header column-names]
              [:th (hiccup header)]))]
     (into [:tbody]
           (for [row row-vectors]
             (into [:tr]
                   (for [column row]
                     [:td (hiccup column)]))))]))

(defmethod hiccup :kind/hiccup [{:keys [value]}]
  (util/expand-hiccup value hiccup))
