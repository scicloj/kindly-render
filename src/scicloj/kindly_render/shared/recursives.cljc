(ns scicloj.kindly-render.shared.recursives
  (:require
   [scicloj.kindly-render.shared.walk :as walk]))


(defn render-vector [{:as note :keys [value]} render-fn]
  (walk/render-data-recursively note :kind/vector value render-fn))

(defn render-map [{:as note :keys [value]} render-fn]
  ;; kindly.css puts kind-map in a grid
  (walk/render-data-recursively note :kind/map (apply concat value) render-fn))

(defn render-set [{:as note :keys [value]} render-fn]
  (walk/render-data-recursively note :kind/set value render-fn))

(defn render-seq [{:as note :keys [value]} render-fn]
  (walk/render-data-recursively note :kind/seq value render-fn))

;; Special data type hiccup that needs careful expansion

(defn render-hiccup [{:as note :keys [value]} render-fn]
  (walk/render-hiccup-recursively note render-fn))

(defn render-table [{:as note :keys [value]} render-fn]
  (if (contains?
       (->> note :advice (map first) set)
       :kind/dataset)
    (render-fn (assoc note :kind :kind/dataset))
    (walk/render-table-recursively note render-fn)))

(defn render-kind-fn [{:as note :keys [value form]} render-fn]
  (let [new-note
        (if (vector? value)
          (let [f (first value)]
            (render-fn {:value (apply f (rest value))
                        :form form}))

          (let [f (or (:kindly/f value)
                      (-> note :kindly/options :kindly/f))]
            (render-fn {:value (f (dissoc value :kindly/f))
                        :form form})))]

    (assoc note
           :hiccup (:hiccup new-note))))
