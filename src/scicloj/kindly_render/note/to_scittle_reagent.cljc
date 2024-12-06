(ns scicloj.kindly-render.note.to-scittle-reagent
  (:require [scicloj.kindly-render.note.to-hiccup-js :as to-hiccup-js]
            [scicloj.kindly-render.shared.walk :as walk]))

(defmulti render-advice :kind)

(defn render [note]
  (-> (walk/advise-deps note)
      (render-advice)))

;; fallback to hiccup-js
(defmethod render-advice :default [note]
  (to-hiccup-js/render note))

;; Data types that can be recursive

(defmethod render-advice :kind/vector [{:keys [value]}]
  (walk/render-data-recursively {:class "kind-vector"} value render-advice))

(defmethod render-advice :kind/map [{:keys [value]}]
  (walk/render-data-recursively {:class "kind-map"} (apply concat value) render-advice))

(defmethod render-advice :kind/set [{:keys [value]}]
  (walk/render-data-recursively {:class "kind-set"} value render-advice))

(defmethod render-advice :kind/seq [{:keys [value]}]
  (walk/render-data-recursively {:class "kind-seq"} value render-advice))

;; Special data type hiccup that needs careful expansion

(defmethod render-advice :kind/hiccup [{:keys [value]}]
  (walk/render-hiccup-recursively value render-advice))
