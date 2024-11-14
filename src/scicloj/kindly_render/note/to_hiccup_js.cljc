(ns scicloj.kindly-render.note.to-hiccup-js
  (:require [clojure.pprint :as pprint]
            [clojure.string :as str]
            [scicloj.kindly-render.shared.walk :as walk]
            [scicloj.kindly-render.shared.util :as util]
            [scicloj.kindly-render.note.to-hiccup :as to-hiccup])
  (:import (clojure.lang IDeref)))

(defmulti render-advice :kind)

(defn render [note]
  (-> (walk/derefing-advise note)
      (render-advice)))

;; fallback to regular hiccup
(defmethod render-advice :default [note]
  (to-hiccup/render note))

(def ^:dynamic *deps* (atom #{}))

(defn deps [& ks]
  (swap! *deps* into ks))

;; TODO: Should be configurable
;; TODO: maybe a way to use npm?
(def dep-includes
  {:portal          ["portal-main.js"]
   :scittle         ["https://scicloj.github.io/scittle/js/scittle.js"]
   :scittle-reagent ["https://scicloj.github.io/scittle/js/scittle.reagent.js"]
   :reagent         ["https://unpkg.com/react@18/umd/react.production.min.js"
                     "https://unpkg.com/react-dom@18/umd/react-dom.production.min.js"
                     "https://scicloj.github.io/scittle/js/scittle.js"]
   :vega            ["https://cdn.jsdelivr.net/npm/vega@5"
                     "https://cdn.jsdelivr.net/npm/vega-lite@5"
                     "https://cdn.jsdelivr.net/npm/vega-embed@6"]
   :datatables      ["js"
                     "css"]
   :echarts         ["https://cdn.jsdelivr.net/npm/echarts@5.4.1/dist/echarts.min.js"]})

(def transitive-deps
  {:datatables      #{:jquery}
   :reagent         #{:scittle-reagent}
   :scittle-reagent #{:scittle}})

(defn include-js []
  ;; TODO: sort is just lucky because reagent comes before scittle
  (distinct (mapcat dep-includes (sort @*deps*))))

(defmethod render-advice :kind/hidden [note])

(defn vega [value]
  (deps :vega)
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
  (deps :scittle)
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
  (deps :reagent :scittle-reagent)
  (let [id (gen-id)]
    [:div {:id id}
     (scittle [(list 'dom/render
                     (if (vector? value) value [value])
                     (list 'js/document.getElementById id))])]))

(defmethod render-advice :kind/scittle [{:keys [form value]}]
  (deps :scittle)
  ;; quoted forms will be unquoted in the scittle output because they should have no effect in Clojure
  ;; unquoted forms may cause effects in Clojure and appear as a scittle script
  (let [forms (if (and (seq? form) (= 'quote (first form)))
                [value]
                (if (vector? form) form [form]))]
    (scittle forms)))

(def portal-enabled?
  (try
    (require 'scicloj.kind-portal.v1.api)
    true
    (catch Throwable _ false)))

(defn portal [note]
  (if portal-enabled?
    (do
      (deps :portal)
      (let [portal-value (apply (ns-resolve 'scicloj.kind-portal.v1.api 'prepare) note)
            value-str (binding [*print-meta* true]
                        (pr-str portal-value))]
        [:div
         [:script
          (str "portal.extensions.vs_code_notebook.activate().renderOutputItem(
  {\"mime\": \"x-application/edn\",
   \"text\": (() => " (pr-str value-str) ")},
  document.currentScript.parentElement);")]]))
    [:pre "kind-portal not included in dependencies"]))

(defmethod render-advice :kind/portal [{:keys [value]}]
  ;; TODO: it isn't clear that value must be a vector wrapper, but it probably is???
  ;; Wouldn't it be nicer if :tool/portal was orthogonal to :kind/vega etc...
  ;; what about :kind/chart, :grammar/vega, :tool/portal ... too much?
  ;; Also, this conflicts with choosing a tool to render with (what if I want everything to be a portal?)

  ;; TODO: kind/portal is replacing kind/hiccup
  (portal {:value (first value)}))

;; Data types that can be recursive

(defmethod render-advice :kind/vector [{:keys [value]}]
  (walk/render-data-recursively {:class "kind_vector"} value render-advice))

(defmethod render-advice :kind/map [{:keys [value]}]
  (walk/render-data-recursively {:class "kind_map"} (apply concat value) render-advice))

(defmethod render-advice :kind/set [{:keys [value]}]
  (walk/render-data-recursively {:class "kind_set"} value render-advice))

(defmethod render-advice :kind/seq [{:keys [value]}]
  (walk/render-data-recursively {:class "kind_seq"} value render-advice))

;; Special data type hiccup that needs careful expansion

(defmethod render-advice :kind/hiccup [{:keys [value]}]
  (walk/render-hiccup-recursively value render-advice))

(defmethod render-advice :kind/tex [{:keys [value]}]
  (deps :katex)
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
  (deps :cytoscape)
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
  (deps :echarts)
  [:div {:style (extract-style note)}
   [:script (format "
{
var myChart = echarts.init(document.currentScript.parentElement);
myChart.setOption(%s);
};"
                    (util/json-str value))]])

(defmethod render-advice :kind/plotly [{:keys [value]}]
  (deps :plotly)
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
  (deps :htmlwidgets-ggplotly)
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
  (deps :highcharts)
  [:div
   [:script
    (format "Highcharts.chart(document.currentScript.parentElement, %s);"
            (util/json-str value))]])
