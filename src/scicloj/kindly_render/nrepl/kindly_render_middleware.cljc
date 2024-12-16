(ns scicloj.kindly-render.nrepl.kindly-render-middleware
  (:require [nrepl.core :as nrepl]
            [nrepl.middleware :as middleware]
            [nrepl.transport :as t]
            [nrepl.middleware.interruptible-eval :as eval]
            [nrepl.middleware.print :as print]
            [scicloj.kindly-render.notes.to-html-page :as to-html-page]
            [scicloj.kindly-render.notes.to-hiccup-page :as to-hiccup-page]
            [scicloj.kindly-render.entry.hiccups :as hiccups])
  (:import (nrepl.transport Transport)))

;; kindly render specific
;; this should be a separate middleware,
;; clay should have its own middleware as well
(defn safe-render [form value]
  (try
    (let [note {:form  form
                :value value}
          notebook (hiccups/with-hiccups {:notes [note]})
          ;; TODO: maybe this shows the interface is not perfect yet
          hiccups (to-hiccup-page/hiccups notebook)]
      (if (seq hiccups)
        (assoc notebook :html (to-html-page/page notebook))
        notebook))
    (catch Exception ex
      (println "OH NO!!!"))))

(defn eval*
  "Returns a wrapping map with extra information as value"
  [form]
  (let [value (clojure.core/eval form)
        notebook (safe-render form value)
        kind (get-in notebook [:notes 0 :kind])
        html (:html notebook)]
    ;; actually can be done with metadata?? not always - but if it can it's fine
    {:value              (if (and html kind)
                           ;; TODO: Cursive should use :kindly-render/html instead
                           (tagged-literal 'cursive/html {:html html})
                           value)
     :meta               (meta value)
     :form-meta          (meta form)
     :kindly/kind        kind
     ;; TODO: html can't be modified easily (inserting CSS), requires some string splicing
     ;; but that's probably fine
     :kindly-render/html html
     ::print/keys        #{:value :meta :form-meta :kindly/kind}}))

;; kindly is a way to annotate visualizations
;; this middleware detects them and sends html
;; but we need to annotate the html
;; why not annotate it with kindly?
;; (because we lose the original kind)
;;
;; maybe kindly can be used in multiple places
;;



(defn unwrap-value
  "Collapses the extra fields from :value into msg"
  [msg]
  (if (and (contains? msg :value)
           (map? (:value msg))
           (contains? (:value msg) ::print/keys))
    (merge msg (:value msg))
    msg))

(defn unwrapping-transport
  "Updates eval messages collapsing the extra information into the message"
  [transport]
  (reify Transport
    (recv [this] (t/recv transport))
    (recv [this timeout] (t/recv transport timeout))
    (send [this msg] (t/send transport (unwrap-value msg)))))

(defn kindly-render-handler
  "A handler that adds visualization information into the msg result of eval ops"
  [{:as req :keys [transport]}]
  (assoc req
    :eval `eval*
    :transport (unwrapping-transport transport)))

(defn wrap-kindly-render
  "Middleware that adds visualizations in :kindly.render/html"
  [handle]
  (fn [msg]
    (handle (kindly-render-handler msg))))

;; The transport must be wrapped before print/wrap-print
;; The expanding eval must be added before interruptible-eval
(middleware/set-descriptor! #'wrap-kindly-render
                            {:requires #{#'print/wrap-print}
                             :expects  #{#'eval/interruptible-eval}
                             :handles  {}})

;; Testing
(comment
  (require '[nrepl.server :as server])
  (def server (server/start-server :port 7888
                                   :handler (server/default-handler #'wrap-kindly-render)))

  (defn show-html [html]
    (tagged-literal 'cursive/html {:html html}))
  (require '[nrepl.core :as nrepl])
  (with-open [conn (nrepl/connect :port 7888)]
    (-> (nrepl/client conn 1000)
        (nrepl/message {:op   "eval"
                        :code "^{:kindly/kind :kind/hiccup}[:svg [:circle {:r 100}]]"})
        ;;(nrepl/response-values)
        (first)
        (:kindly-render/html)
        (show-html)))

  (tagged-literal 'cursive/html {:html "<h1>hi</h1>"})


  (server/stop-server server))
