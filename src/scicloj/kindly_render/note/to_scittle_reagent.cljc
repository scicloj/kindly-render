(ns scicloj.kindly-render.note.to-scittle-reagent
  (:require [scicloj.kindly-render.note.to-hiccup :as to-hiccup]
            [scicloj.kindly-render.util :as util]))

(defmulti render* :kind)

(defn render [note]
  (render* note))

(defmethod render* :default [note]
  (to-hiccup/render note))

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
