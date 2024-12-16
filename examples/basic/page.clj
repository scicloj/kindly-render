(ns basic.page
  (:require [scicloj.kindly-render.notes.to-html-page :as to-html-page]
            [scicloj.kindly-render.notes.to-markdown-page :as to-markdown-page]
            [scicloj.kindly.v4.kind :as kind]
            [tech.v3.dataset :as td]))

(def chart
  (kind/echarts {:title   {:text "Echarts Example"}
                 :tooltip {}
                 :legend  {:data ["sales"]}
                 :xAxis   {:data ["Shirts", "Cardigans", "Chiffons", "Pants", "Heels", "Socks"]}
                 :yAxis   {}
                 :series  [{:name "sales"
                            :type "bar"
                            :data [5 20 36 10 10 20]}]}
                {:style {:height "200px"}}))

(def hiccup-list
  (kind/hiccup
    [:div {:style {:background   "#efe9e6"
                   :border-style :solid}}
     [:ul
      [:li "one"]
      [:li "two"]
      [:li "three"]]]))

(def portal
  (kind/portal {:foo "bar"}))

(def dataset
  (td/->dataset {:x (range 5)
                 :y (repeatedly 5 rand)}
                {:dataset-name "my dataset"}))

(def notebook
  {:kindly/options {:deps #{:kindly :clay}}
   :notes [
           ;; kind-portal is not loaded, so this should render a short
           ;; message explaining its absence
           ;;{:value portal}

           ;; single kind
           {:value hiccup-list}

           ;; basic hiccup
           {:value (kind/hiccup [:div {} chart chart])}

           ;; nested hiccup
           {:value (kind/hiccup [:div {}
                                 [:div chart chart]])}

           ;; nested vector inside hiccup
           {:value (kind/hiccup [:div {}
                                 [chart chart]])}

           ;; What happens to markdown?
           {:value (kind/md "Hello, this is some text")}

           {:value dataset}

           ;; I want to define a function available in scittle (ClojureScript) and Clojure
           ;; run it/test it in Clojure, and run it in the notebook
           ;; use case: Physics simulation of gravity.
           ;; I don't want to quote my function.
           ;; In this case we want to make use of form instead of value.
           {:form (with-meta '(defn f [x] (* x 0.9))
                             {:kindly/kind :kind/scittle})}

           ;; quoted scittle still creates a script, it does not have side effects in Clojure
           {:value (with-meta '(defn g [x] (+ x 2))
                              {:kindly/kind :kind/scittle})}

           ;; scittle and reagent in hiccup
           {:value (kind/hiccup [:div "Hello world"
                                 ;; scittle
                                 '[(println "hello world from hiccup scittle")
                                   (defn my-ui [props] [:div "This component was defined previously"])]
                                 ;; reagent component
                                 '[my-ui]
                                 ;; reagent inline component
                                 '[(fn [props]
                                     [:div "I'm a reagent component"
                                      [:svg [:circle {:r 50}]]])]])}

           ]})

(defn -main [& args]
  (->> (to-markdown-page/render-notebook notebook)
       (spit "basic.md"))
  (->> (to-html-page/render-notebook notebook)
       (spit "basic.html")))

(comment
  (-main))

;; TODO:
;; 1. Does it work for Clay?
;; 2. Does it work from ClojureScript?
