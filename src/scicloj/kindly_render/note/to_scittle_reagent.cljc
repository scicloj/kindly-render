(ns scicloj.kindly-render.note.to-scittle-reagent
  (:require [scicloj.kindly-render.note.to-hiccup-js :as to-hiccup-js]
            [scicloj.kindly-render.note.to-hiccup :as to-hiccup]
            [scicloj.kindly-render.shared.walk :as walk]))

(defmulti render-advice :kind)

(defn render [note]
  (-> (walk/advise-with-deps note)
      (render-advice)
      (to-hiccup/kindly-style)))

;; fallback to hiccup-js
(defmethod render-advice :default [note]
  (to-hiccup-js/render note))

;; Data types that can be recursive

(defmethod render-advice :kind/vector [{:as note :keys [value]}]
  (walk/render-data-recursively note {:class "kind-vector"} value render-advice))

(defmethod render-advice :kind/map [{:as note :keys [value]}]
  (walk/render-data-recursively note {:class "kind-map"} (apply concat value) render-advice))

(defmethod render-advice :kind/set [{:as note :keys [value]}]
  (walk/render-data-recursively note {:class "kind-set"} value render-advice))

(defmethod render-advice :kind/seq [{:as note :keys [value]}]
  (walk/render-data-recursively note {:class "kind-seq"} value render-advice))

;; Special data type hiccup that needs careful expansion

(defmethod render-advice :kind/hiccup [note]
  (walk/render-hiccup-recursively note render-advice))
