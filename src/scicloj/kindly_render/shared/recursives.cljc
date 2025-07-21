(ns scicloj.kindly-render.shared.recursives
  (:require
   [scicloj.kindly-render.shared.walk :as walk]))

;; TODO: move to walk
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
