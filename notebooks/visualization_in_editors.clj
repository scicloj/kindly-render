(ns visualization-in-editors
  (:require [scicloj.kindly.v4.kind :as kind]
            [tablecloth.api :as tc]
            [scicloj.tableplot.v1.plotly :as plotly]))

;; Notebooks are namespaces that mix *narrative, code, and visualizations*.

;; ## Why visualize?

(def distances
  (tc/dataset [["shop" 0.2]
               ["library" 0.7]
               ["airport" 12]
               #_["DC" 204]
               #_["London" 3461]
               #_["Moon" 338900]]
              {:column-names [:location :distance]}))

(plotly/layer-bar distances
                  {:=y :distance
                   :=x :location})

(kind/echarts {:title   {:text "Echarts Example"}
               :tooltip {}
               :legend  {:data ["sales"]}
               :xAxis   {:data ["Shirts", "Cardigans", "Chiffons", "Pants", "Heels", "Socks"]}
               :yAxis   {}
               :series  [{:name "sales"
                          :type "bar"
                          :data [5 20 36 10 10 20]}]})

;; ## Why Kindly?

;; **No rendering dependency in user code**

;; * Uniformity: A single interface for multiple tools like Clay, Portal, and Clerk.
;; * Many visualizations: From simple Markdown to advanced visualizations like Vega or Cytoscape.
;; * Community-Driven: Already powering major documentation projects like Tablecloth and Fastmath.

;; **Already working well for the SciCloj community**

;; ## Not the only way to visualize

;; * Not an inspector (but can request one)

;; can make a command that runs joyride

(kind/portal
  {:a [1 2 3]
   :b [4 5 6]})

;; ## When to use Kindly?

;; Everywhere

;; ## What about Clerk?

;; Different features, "locked in" to producing React
