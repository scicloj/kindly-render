(ns scicloj.kindly-render.value.to-hiccup-js
  (:require #?(:clj [clojure.data.json :as json])
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [scicloj.kind-portal.v1.impl :as kpi]
            [scicloj.kindly-render.util :as util]
            [scicloj.kindly-render.value.to-hiccup :as to-hiccup])
  (:import (clojure.lang IDeref)))

(defmulti hiccup-js :kind)

(defmethod hiccup-js :default [note]
  (to-hiccup/hiccup note))

(def ^:dynamic *deps* (atom #{}))

(defn deps [& ks]
  (swap! *deps* into ks))

(def dep-includes
  {:portal  ["portal-main.js"]
   :scittle ["https://scicloj.github.io/scittle/js/scittle.js"]
   :reagent ["https://unpkg.com/react@18/umd/react.production.min.js"
             "https://unpkg.com/react-dom@18/umd/react-dom.production.min.js"
             "https://scicloj.github.io/scittle/js/scittle.js"
             "https://scicloj.github.io/scittle/js/scittle.reagent.js"]
   :vega    ["https://cdn.jsdelivr.net/npm/vega@5"
             "https://cdn.jsdelivr.net/npm/vega-lite@5"
             "https://cdn.jsdelivr.net/npm/vega-embed@6"]})

(defn include-js []
  (distinct (mapcat dep-includes @*deps*)))

(defmethod hiccup-js :kind/hidden [note])

(defn vega [value]
  (deps :vega)
  [:div {:style {:width "100%"}}
   [:script (str "vegaEmbed(document.currentScript.parentElement, "
                 #?(:clj (json/write-str value)
                    :cljs (clj->js value))
                 ");")]])

(defmethod hiccup-js :kind/vega [{:keys [value]}]
  (vega value))

(defmethod hiccup-js :kind/vega-lite [{:keys [value]}]
  (vega value))

(defn format-code [form]
  (binding [pprint/*print-pprint-dispatch* pprint/code-dispatch]
    (with-out-str (pprint/pprint form))))

(defn scittle [forms]
  (deps :scittle)
  [:script {:type "application/x-scittle"}
   (str/join \newline (map format-code forms))])

(defn no-spaces [s]
  (str/replace s #"\s" "-"))

(defn gen-id []
  (str (no-spaces *scope-name*) "-" (swap! *counter* inc)))

(defn reagent [v]
  (deps :reagent)
  (let [id (gen-id)]
    [:div {:id id}
     (-> [(list 'dom/render v (list 'js/document.getElementById id))]
         (scittle))]))

(defmethod hiccup-js :kind/reagent [{:keys [value]}]
  (if (vector? value)
    (reagent value)
    (reagent [value])))

(defn portal [note]
  (deps :portal)
  (let [portal-value (kpi/prepare note)
        value-str (binding [*print-meta* true]
                    (pr-str portal-value))]
    [:div
     [:script
      (str "portal.extensions.vs_code_notebook.activate().renderOutputItem(
  {\"mime\": \"x-application/edn\",
   \"text\": (() => " (pr-str value-str) ")},
  document.currentScript.parentElement);")]]))

(defmethod hiccup-js :kind/portal [{:keys [value]}]
  ;; TODO: it isn't clear that value must be a vector wrapper, but it probably is???
  ;; Wouldn't it be nicer if :tool/portal was orthogonal to :kind/vega etc...
  ;; what about :kind/chart, :grammar/vega, :tool/portal ... too much?
  ;; Also, this conflicts with choosing a tool to render with (what if I want everything to be a portal?)

  ;; TODO: kind/portal is replacing kind/hiccup
  (portal {:value (first value)}))

(defmethod hiccup-js :kind/hiccup [{:keys [value]}]
  (util/expand-hiccup value hiccup-js))
