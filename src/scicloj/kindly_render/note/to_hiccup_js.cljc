(ns scicloj.kindly-render.note.to-hiccup-js
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [scicloj.kindly-render.shared.walk :as walk]
            [scicloj.kindly-render.shared.util :as util]
            [scicloj.kindly-render.note.to-hiccup :as to-hiccup])
  (:import (clojure.lang IDeref)))

(defmulti render-advice :kind)

(defn render [note]
  (walk/advise-render-style note render-advice))

;; fallback to regular hiccup
;; fallback to regular hiccup
(defmethod render-advice :default [note]
  (to-hiccup/render-advice note))

(defmethod render-advice :kind/hidden [note]
  note)

(defn vega [value]
  [:div {:style {:width "100%"}}
   [:script (str "vegaEmbed(document.currentScript.parentElement, "
                 (util/json-str value)
                 ");")]])

(defmethod render-advice :kind/vega [{:as note :keys [value]}]
  (->> (vega value)
       (assoc note :hiccup)))

(defmethod render-advice :kind/vega-lite [{:as note :keys [value]}]
  (->> (vega value)
       (assoc note :hiccup)))

(def ^:dynamic *id-counter*
  "starting id for id generation,
  for consistent ids consider binding to 0 per html page"
  (atom 0))

(def ^:dynamic *id-prefix*
  "an optional prefix when generating html element ids"
  nil)

(defn no-spaces [s]
  (when s (str/replace s #"\s" "-")))

(defn gen-id []
  (str (no-spaces *id-prefix*) "-" (swap! *id-counter* inc)))

(defmethod render-advice :kind/reagent [{:as note :keys [value]}]
  (->> (let [id (gen-id)]
         [:div {:id id}
          (util/scittle [(list 'dom/render
                               (if (vector? value) value [value])
                               (list 'js/document.getElementById id))])])
       (assoc note :hiccup)))

(defmethod render-advice :kind/scittle [{:as note :keys [form code value]}]
  ;; quoted forms will be unquoted in the scittle output because they should have no effect in Clojure
  ;; unquoted forms may cause effects in Clojure and appear as a scittle script
  (->> (let [form (if (and (nil? form) code)
                    (edn/read-string code)
                    form)
             forms (if (or (and (seq? form) (= 'quote (first form)))
                           (nil? form))
                     (if (vector? value) value [value])
                     (if (vector? form) form [form]))]
         (util/scittle forms))
       (assoc note :hiccup)))

(defmethod render-advice :kind/portal [{:keys [value] :as note}]
  (->> [:div
        [:script
         (str "portal_api.embed().renderOutputItem(
  {'mime': 'x-application/edn',
   'text': (() => " (pr-str (pr-str value)) ")},
  document.currentScript.parentElement);")]]
       (assoc note :hiccup)))

;; Data types that can be recursive

(defmethod render-advice :kind/vector [{:as note :keys [value]}]
  (walk/render-data-recursively note {:class "kind-vector"} value render))

(defmethod render-advice :kind/map [{:as note :keys [value]}]
  ;; kindly.css puts kind-map in a grid
  (walk/render-data-recursively note {:class "kind-map"} (apply concat value) render))

(defmethod render-advice :kind/set [{:as note :keys [value]}]
  (walk/render-data-recursively note {:class "kind-set"} value render))

(defmethod render-advice :kind/seq [{:as note :keys [value]}]
  (walk/render-data-recursively note {:class "kind-seq"} value render))

;; Special data type hiccup that needs careful expansion

(defmethod render-advice :kind/hiccup [note]
  (walk/render-hiccup-recursively note render))

(defmethod render-advice :kind/table [{:as note :keys [value]}]
  (walk/render-table-recursively value render))

(defmethod render-advice :kind/tex [{:as note :keys [value]}]
  (->> (into [:div]
             (for [s (if (vector? value) value [value])]
               [:div
                [:script
                 (format "katex.render(%s, document.currentScript.parentElement, {throwOnError: false});"
                         (util/json-str s))]]))
       (assoc note :hiccup)))

(def default-style {:height "400px"
                    :width  "100%"})

(defmethod render-advice :kind/cytoscape [{:as note :keys [value]}]
  (->> [:div {:style default-style}
        [:script (format "
{
value = %s;
value['container'] = document.currentScript.parentElement;
cytoscape(value);
};"
                         (util/json-str value))]]
       (assoc note :hiccup)))

(defmethod render-advice :kind/echarts [{:as note :keys [value]}]
  (->> [:div {:style default-style}
        [:script (format "
{
var myChart = echarts.init(document.currentScript.parentElement);
myChart.setOption(%s);
};"
                         (util/json-str value))]]
       (assoc note :hiccup)))

(defmethod render-advice :kind/plotly [{:as note :keys [value]}]
  (->> (let [{:keys [data layout config]
              :or   {layout {}
                     config {}}} value]
         [:div
          [:script
           (format
             "Plotly.newPlot(document.currentScript.parentElement,
     %s, %s, %s);"
             (util/json-str data)
             (util/json-str layout)
             (util/json-str config))]])
       (assoc note :hiccup)))

(defmethod render-advice :kind/ggplotly [{:as note :keys [value]}]
  (->> (let [id (gen-id)]
         [:div {:class "plotly html-widget html-fill-item-overflow-hidden html-fill-item"
                :id    id
                :style default-style}
          [:script {:type     "application/htmlwidget-sizing"
                    :data-for id}
           (util/json-str {:viewer  {:width   "100%"
                                     :height  400
                                     :padding "0"
                                     :fille   true}
                           :browser {:width   "100%"
                                     :height  400
                                     :padding "0"
                                     :fille   true}})]
          [:script {:type     "application/json"
                    :data-for id}
           (util/json-str value)]])
       (assoc note :hiccup)))

(defmethod render-advice :kind/highcharts [{:as note :keys [value]}]
  (->> [:div
        [:script
         (format "Highcharts.chart(document.currentScript.parentElement, %s);"
                 (util/json-str value))]]
       (assoc note :hiccup)))
