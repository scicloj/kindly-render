(ns basic.page
  (:require [scicloj.kindly-advice.v1.api :as kindly-advice]
            [scicloj.kindly-render.notes.to-html-page :as to-html-page]
            [scicloj.kindly-render.notes.to-markdown-page :as to-markdown-page]
            [scicloj.kindly.v4.kind :as kind]))

(def chart
  (kind/echarts {:title   {:text "Echarts Example"}
                 :tooltip {}
                 :legend  {:data ["sales"]}
                 :xAxis   {:data ["Shirts", "Cardigans", "Chiffons",
                                  "Pants", "Heels", "Socks"]}
                 :yAxis   {}
                 :series  [{:name "sales"
                            :type "bar"
                            :data [5 20 36
                                   10 10 20]}]}))

(def hiccup-list
  (kind/hiccup
    [:div {:style {:background "#efe9e6"
                   :border-style :solid}}
     [:ul
      [:li "one"]
      [:li "two"]
      [:li "three"]]]))

(def notebook
  {:notes [
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
