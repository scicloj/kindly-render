(ns basic.page
  (:require [scicloj.kindly-advice.v1.api :as kindly-advice]
            [scicloj.kindly-render.notes.to-html-page :as h]
            [scicloj.kindly-render.notes.to-markdown-page :as m]
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

(def notebook
  {:notes [
           ;; single kind
           ;;{:value chart}

           ;; basic hiccup
           ;;{:value (kind/hiccup [:div {} chart chart])}

           ;; nested hiccup
           #_{:value (kind/hiccup [:div {}
                                   [:div chart chart]])}

           ;; nested vector inside hiccup
           {:value (kind/hiccup [:div {}
                                 [chart chart]])}

           ;; What happens to markdown?
           {:value (kind/md "Hello, this is some text")}

           ]})

(defn -main [& args]
  ;; TODO: call advise if there is no advice already later in the chain.
  (let [x (update notebook :notes
                  (fn [notes]
                    (map kindly-advice/advise notes)))]
    (->> (m/notes-to-md x)
         (spit "basic.md"))

    (->> (h/notes-to-html x)
         (spit "basic.html"))))

(comment
  (-main))

;; TODO:
;; 1. Does it work for Clay?
;; 2. Does it work from ClojureScript?

;; TODO: Nested stuff
