(ns scicloj.kindly-render.note.to-hiccup-inline-js-test
  (:require
   [clojure.string :as str]
   [clojure.string :as s]
   [hiccup.core :as hiccup]
   ;[reagent.core]
   [scicloj.kindly-advice.v1.api :as kindly-advice]
   [scicloj.kindly-render.note.to-hiccup :as to-hiccup]
   [scicloj.kindly-render.note.to-hiccup-js :as to-hiccup-js]
   [scicloj.kindly-render.shared.walk :as walk]
   [scicloj.kindly.v4.kind :as kind]
   [scicloj.tableplot.v1.plotly :as plotly]
   [scicloj.metamorph.ml.rdatasets :as rdatasets]
   [tablecloth.api :as tc]
   [scicloj.kindly-render.note.to-hiccup-inline-js :as to-hiccup-inline-js]
   [clojure.test :refer [deftest is testing]]))



(defn multi-nth
  [v indexes]
  (reduce (fn [coll idx]
            (nth coll idx))
          v
          indexes))



(def vl-spec
  {:encoding
   {:y {:field "y", :type "quantitative"},
    :size {:value 400},
    :x {:field "x", :type "quantitative"}},
   :mark {:type "circle", :tooltip true},
   :width 400,
   :background "floralwhite",
   :height 100,
   :data {:values "x,y\n1,1\n2,-4\n3,9\n", :format {:type "csv"}}})

(def cs
  (kind/cytoscape
   {:elements {:nodes [{:data {:id "a" :parent "b"} :position {:x 215 :y 85}}
                       {:data {:id "b"}}
                       {:data {:id "c" :parent "b"} :position {:x 300 :y 85}}
                       {:data {:id "d"} :position {:x 215 :y 175}}
                       {:data {:id "e"}}
                       {:data {:id "f" :parent "e"} :position {:x 300 :y 175}}]
               :edges [{:data {:id "ad" :source "a" :target "d"}}
                       {:data {:id "eb" :source "e" :target "b"}}]}
    :style [{:selector "node"
             :css {:content "data(id)"
                   :text-valign "center"
                   :text-halign "center"}}
            {:selector "parent"
             :css {:text-valign "top"
                   :text-halign "center"}}
            {:selector "edge"
             :css {:curve-style "bezier"
                   :target-arrow-shape "triangle"}}]
    :layout {:name "preset"
             :padding 5}}))

(def vega-spec
  {:$schema "https://vega.github.io/schema/vega/v5.json"
   :width 400
   :height 200
   :padding 5
   :data {:name "table"
          :values [{:category :A :amount 28}
                   {:category :B :amount 55}
                   {:category :C :amount 43}
                   {:category :D :amount 91}
                   {:category :E :amount 81}
                   {:category :F :amount 53}
                   {:category :G :amount 19}
                   {:category :H :amount 87}]}
   :signals [{:name :tooltip
              :value {}
              :on [{:events "rect:mouseover"
                    :update :datum}
                   {:events "rect:mouseout"
                    :update "{}"}]}]
   :scales [{:name :xscale
             :type :band
             :domain {:data :table
                      :field :category}
             :range :width
             :padding 0.05
             :round true}
            {:name :yscale
             :domain {:data :table
                      :field :amount}
             :nice true
             :range :height}]
   :axes [{:orient :bottom :scale :xscale}
          {:orient :left :scale :yscale}]
   :marks {:type :rect
           :from {:data :table}
           :encode {:enter {:x {:scale :xscale
                                :field :category}
                            :width {:scale :xscale
                                    :band 1}
                            :y {:scale :yscale
                                :field :amount}
                            :y2 {:scale :yscale
                                 :value 0}}
                    :update {:fill
                             {:value :steelblue}}
                    :hover {:fill
                            {:value :red}}}}})

(def plotly-data
  (let [n 20
        walk (fn [bias]
               (->> (repeatedly n #(-> (rand)
                                       (- 0.5)
                                       (+ bias)))
                    (reductions +)))]
    {:data [{:x (walk 1)
             :y (walk -1)
             :z (map #(* % %)
                     (walk 2))
             :type :scatter3d
             :mode :lines+markers
             :opacity 0.2
             :line {:width 10}
             :marker {:size 20
                      :colorscale :Viridis}}]}))

;; (def people-as-maps
;;   (->> (range 29)
;;        (mapv (fn [_]
;;                {:preferred-language (["clojure" "clojurescript" "babashka"]
;;                                      (rand-int 3))
;;                 :age (rand-int 100)}))))

;; (def people-as-vectors
;;   (->> people-as-maps
;;        (mapv (juxt :preferred-language :age))))

;; (def people-as-dataset
;;   (tc/dataset people-as-maps))

;; (defn fetch-dataset [dataset-name]
;;   (-> dataset-name
;;       (->> (format "https://vincentarelbundock.github.io/Rdatasets/csv/%s.csv"))
;;       (tc/dataset {:key-fn (fn [k]
;;                              (-> k
;;                                  str/lower-case
;;                                  (str/replace #"\." "-")
;;                                  keyword))})
;;       (tc/set-dataset-name dataset-name)))

;; (def iris
;;   (fetch-dataset "datasets/iris"))



(deftest kind-vega-lite-works
  (is (= 
       true
       (str/includes?
        (->
         (to-hiccup-inline-js/render {:value (kind/vega-lite vl-spec)})
         :hiccup
         (nth 2)
         (nth 2)
         second)
        "vega")
       )))

;; ;(to-hiccup-inline-js/render {:form })


(deftest kind-vega-works

    (is (= true
           (str/includes?
            (->
             (to-hiccup-inline-js/render {:value (kind/vega vega-spec)})
             :hiccup
             (nth 2)
             (nth 2)
             second)
            "vega"))

        ))



(deftest kind-plotly-works
  (is (= true
         (str/includes?
          (->
           (to-hiccup-inline-js/render {:value (kind/plotly plotly-data)})
           :hiccup
           (nth 2)
           first
           second)
          "Plotly.newPlot")))

  (is (= true
         (str/starts-with?
          (->
           (to-hiccup-inline-js/render {:value (kind/plotly plotly-data {:style {:width 100
                                                                                :height 100}})})
           :hiccup
           (nth 2)
           first
           second)
          "  \n  var"))))

(deftest kind-reagent-works
  (is (= true 
         (str/includes?
          (->
           (to-hiccup-inline-js/render {:value
                                        (kind/reagent
                                         ['(fn [numbers]
                                             [:p {:style {:background "#d4ebe9"}}
                                              (pr-str (map inc numbers))])
                                          (vec (range 10))])})
           :hiccup
           (nth 2)
           (nth 4))
          "reagent.dom/render"))))

(deftest kind-reagent-supports-deps
  (is (= true
         (str/includes?
          (->
           (to-hiccup-inline-js/render {:value
                                        (kind/reagent
                                         ['(fn []
                                             [:div {:style {:height "200px"}
                                                    :ref (fn [el]
                                                           (let [m (-> js/L
                                                                       (.map el)
                                                                       (.setView (clj->js [51.505 -0.09])
                                                                                 13))]
                                                             (-> js/L
                                                                 .-tileLayer
                                                                 (.provider "OpenStreetMap.Mapnik")
                                                                 (.addTo m))
                                                             (-> js/L
                                                                 (.marker (clj->js [51.5 -0.09]))
                                                                 (.addTo m)
                                                                 (.bindPopup "A pretty CSS popup.<br> Easily customizable.")
                                                                 (.openPopup))))}])]
    ;; Note we need to mention the dependency:
                                         {:html/deps [:leaflet]})})
           :hiccup
           (nth 2)
           (nth 5))
          "leaflet.js"))))

(deftest kind-tex-works
  (is (= true 
         (str/includes?
          (->
           (to-hiccup-inline-js/render {:value (kind/tex "x^2 + y^2 = z^2")})
           :hiccup
           (multi-nth []))
          "katex"))))



(deftest kind-portal-works
  (is 
   (str/includes?
    (->
     (to-hiccup-inline-js/render { :value 
                                  (kind/portal
                                   {:x (range 3)})}
                                 )
     :hiccup
     (nth 2)
     first
     second)
    ":portal.viewer/inspector")))
       
       
(deftest kind-scittle-works
  (is 
   (str/includes?
    (->
     (to-hiccup-inline-js/render {:value (kind/scittle '(.log js/console "hello"))})
     :hiccup
     (nth 3))
    ".log js/console")) 

  (is (= [:script {:type "application/x-scittle", :class "kind-scittle"} "(print \"hello\")\n"]
         (->
          (to-hiccup-inline-js/render {:value (kind/scittle '(print "hello"))})
          :hiccup
          (nth 3))))

  (is (= [:script "scittle.core.eval_script_tags()"]
         (->
          (to-hiccup-inline-js/render {:value (kind/scittle '(print "hello"))})
          :hiccup
          (nth 4)))) )



(deftest kind-cytoscape-works
  (is 
   (str/includes?
    (->
     (to-hiccup-inline-js/render {:value ^:kind/cytoscape cs})
     :hiccup
     (nth 2)
     first
     second) "cytoscape")))


