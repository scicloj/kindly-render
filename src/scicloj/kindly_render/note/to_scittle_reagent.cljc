(ns scicloj.kindly-render.note.to-scittle-reagent
  (:require [scicloj.kindly-render.note.to-hiccup :as to-hiccup]
            [scicloj.kindly-render.util :as util]))

(defmulti render-advice :kind)

(defn render [note]
  (render-advice (util/derefing-advise note)))

(defmethod render-advice :default [note]
  (to-hiccup/render note))

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
