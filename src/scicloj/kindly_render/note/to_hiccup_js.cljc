(ns scicloj.kindly-render.note.to-hiccup-js
  (:require [clojure.pprint :as pprint]
            [clojure.string :as str]
            [scicloj.kindly-render.shared.walk :as walk]
            [scicloj.kindly-render.shared.util :as util]
            [scicloj.kindly-render.note.to-hiccup :as to-hiccup])
  (:import (clojure.lang IDeref)))

(defmulti render-advice :kind)

(defn render [note]
  (let [advice (walk/advise-deps note)
        hiccup (render-advice advice)]
    (to-hiccup/kindly-style hiccup advice)))

;; fallback to regular hiccup
(defmethod render-advice :default [note]
  (to-hiccup/render-advice note))

(defmethod render-advice :kind/hidden [note])

(defn vega [value]
  [:div {:style {:width "100%"}}
   [:script (str "vegaEmbed(document.currentScript.parentElement, "
                 (util/json-str value)
                 ");")]])

(defmethod render-advice :kind/vega [{:keys [value]}]
  (vega value))

(defmethod render-advice :kind/vega-lite [{:keys [value]}]
  (vega value))

(defn format-code [form]
  (binding [pprint/*print-pprint-dispatch* pprint/code-dispatch]
    (with-out-str (pprint/pprint form))))

(defn scittle [forms]
  [:script {:type "application/x-scittle"}
   (str/join \newline (map format-code forms))])

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

(defmethod render-advice :kind/reagent [{:keys [value]}]
  (let [id (gen-id)]
    [:div {:id id}
     (scittle [(list 'dom/render
                     (if (vector? value) value [value])
                     (list 'js/document.getElementById id))])]))

(defmethod render-advice :kind/scittle [{:keys [form value]}]
  ;; quoted forms will be unquoted in the scittle output because they should have no effect in Clojure
  ;; unquoted forms may cause effects in Clojure and appear as a scittle script
  (let [forms (if (or (and (seq? form) (= 'quote (first form)))
                      (nil? form))
                (if (vector? value) value [value])
                (if (vector? form) form [form]))]
    (scittle forms)))

(defn portal [{:keys [value]}]
  [:div
   [:script
    (str "portal.extensions.vs_code_notebook.activate().renderOutputItem(
  {\"mime\": \"x-application/edn\",
   \"text\": (() => " (pr-str (pr-str value)) ")},
  document.currentScript.parentElement);")]])

(defmethod render-advice :kind/portal [note]
  (portal note))

;; Data types that can be recursive

(defmethod render-advice :kind/vector [{:keys [value]}]
  (walk/render-data-recursively {:class "kind-vector"} value render))

(defmethod render-advice :kind/map [{:keys [value]}]
  (walk/render-data-recursively {:class "kind-map"} (apply concat value) render))

(defmethod render-advice :kind/set [{:keys [value]}]
  (walk/render-data-recursively {:class "kind-set"} value render))

(defmethod render-advice :kind/seq [{:keys [value]}]
  (walk/render-data-recursively {:class "kind-seq"} value render))

;; Special data type hiccup that needs careful expansion

(defmethod render-advice :kind/hiccup [{:keys [value]}]
  (walk/render-hiccup-recursively value render))

(defmethod render-advice :kind/table [{:keys [value]}]
  (walk/render-table-recursively value render))

(defmethod render-advice :kind/tex [{:keys [value]}]
  (into [:div]
        (for [s (if (vector? value) value [value])]
          [:div
           [:script
            (format "katex.render(%s, document.currentScript.parentElement, {throwOnError: false});"
                    (util/json-str s))]])))

(defn extract-style [note]
  (-> note
      :kindly/options
      :element/style
      (or {:height "400px"
           :width  "100%"})))

(defmethod render-advice :kind/cytoscape [{:keys [value]
                                           :as   note}]
  [:div
   {:style (extract-style note)}
   [:script (format "
{
value = %s;
value['container'] = document.currentScript.parentElement;
cytoscape(value);
};"
                    (util/json-str value))]])

;; TODO: standardize json writing
(defmethod render-advice :kind/echarts [{:keys [value]
                                         :as   note}]
  [:div {:style (extract-style note)}
   [:script (format "
{
var myChart = echarts.init(document.currentScript.parentElement);
myChart.setOption(%s);
};"
                    (util/json-str value))]])

(defmethod render-advice :kind/plotly [{:keys [value]}]
  (let [{:keys [data layout config]
         :or   {layout {}
                config {}}} value]
    [:div
     [:script
      (format
        "Plotly.newPlot(document.currentScript.parentElement,
%s, %s, %s);"
        (util/json-str data)
        (util/json-str layout)
        (util/json-str config))]]))

(defmethod render-advice :kind/ggplotly [{:keys [value]
                                          :as   note}]
  (let [id (gen-id)]
    [:div {:class "plotly html-widget html-fill-item-overflow-hidden html-fill-item"
           :id    id
           :style (extract-style note)}
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
      (util/json-str value)]]))

(defmethod render-advice :kind/highcharts [{:keys [value]}]
  [:div
   [:script
    (format "Highcharts.chart(document.currentScript.parentElement, %s);"
            (util/json-str value))]])
