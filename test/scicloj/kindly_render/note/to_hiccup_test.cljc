(ns scicloj.kindly-render.note.to-hiccup-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]
   [scicloj.kindly-render.note.to-hiccup :as to-hiccup]
   [scicloj.kindly.v4.kind :as kind]
   ))


(deftest kind_video
  (is (=
       [:iframe {:src "https://www.youtube.com/embed/DAQnvAgBma8", :allowfullscreen true, :class "kind-video"}]
       (->
        (to-hiccup/render {:value (kind/video
                                   {:youtube-id "DAQnvAgBma8"})})
        :hiccup))))

(deftest kind-pprint
  (is (=
       [:blockquote
        {:class "kind-pprint"}
        [:pre [:code {:class "sourceCode language-clojure printed-clojure"} "{:a \"test\"}\n"]]]


       (->
        (to-hiccup/render {:value (kind/pprint
                                   {:a "test"})})
        :hiccup))))


