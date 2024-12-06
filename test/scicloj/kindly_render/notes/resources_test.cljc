(ns scicloj.kindly-render.notes.resources-test
  (:require [clojure.test :refer [deftest is testing]]
            [scicloj.kindly-render.notes.resources :as resources]))

(deftest resources-test
  ;; TODO: ensure all styles work

  ;; Users need to be able to override deps
  {:kind-deps {:kind/echart {:js ["newversion"]}}
   :js-deps   {:js []}}

  ;; shorthand
  {:deps #{:foo}}
  {:placement {:js :body}
   :deps      #{:foo}}
  {:deps {:js [] :css [] :placement :body}}
  {:deps #{:foo {:js []}}}

  )
