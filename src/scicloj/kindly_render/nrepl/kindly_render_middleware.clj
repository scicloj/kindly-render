(ns scicloj.kindly-render.nrepl.kindly-render-middleware
  (:require [nrepl.core :as nrepl]
            [nrepl.middleware :as middleware]
            [nrepl.transport :as t]
            [nrepl.middleware.interruptible-eval :as eval]
            [nrepl.middleware.print :as print]
            [scicloj.kindly-render.notes.to-html-page :as to-html-page]
            [scicloj.kindly-render.notes.to-hiccup-page :as to-hiccup-page]
            [scicloj.kindly-render.entry.hiccups :as hiccups]
    ;; TODO: these dependencies should not be here, they are for testing convenience only
    #_[scicloj.clay.v2.api :as clay]
    #_[hiccup.core :as hiccup])
  (:import (nrepl.transport Transport)
           (clojure.lang IMeta)))

;; kindly render specific
;; this should be a separate middleware,
;; clay should have its own middleware as well
(defn kindly-render [note]
  (let [notebook (hiccups/with-hiccups {:notes          [note]
                                        :kindly/options {:deps    #{:kindly :clay}
                                                         ;; TODO: there's a problem showing embedded js
                                                         #_#_:package :embed}})
        ;; TODO: maybe there should be an api call to create a single page notebook
        kind (get-in notebook [:notes 0 :kind])
        hiccups (to-hiccup-page/hiccups notebook)]
    (when (and kind (seq hiccups))
      {:html (to-html-page/page notebook)
       :kind (pr-str kind)})))

;; clay render specific
#_(defn clay-render [note]
    (when-let [hiccups (seq (clay/make-hiccup {:single-form       (:form note)
                                               :source-path       (:file (meta (:value note)))
                                               :inline-js-and-css true}))]
      (doto {:html (hiccup/html hiccups)}
        (->> :html (spit "clay-debug.html")))))

;; just preserve metadata for the IDE
(defn meta-only [{:keys [form value]}]
  {:meta      (meta value)
   :form-meta (meta form)})

(defn assoc-meta [value k v]
  "Adds `k` to value metadata if it can"
  (if (instance? IMeta value)
    (with-meta value (assoc (meta value) k v))
    v))

(defn dissoc-meta [value k]
  "Removes `k` from the metadata of value if present"
  (if (instance? IMeta value)
    (with-meta value (dissoc (meta value) k))
    value))

(defn with-extra
  "Adds extra information to be added when the transport sends value from server to client"
  [form value]
  (try
    (let [note {:form  form
                :value value}
          extra (kindly-render note)]
      (when (:html extra) (spit "test.html" (:html extra)))
      (if (:html extra)
        ;; TODO: Cursive should use html in msg instead of tagged literals
        (tagged-literal 'cursive/html (select-keys extra [:html]))
        (assoc-meta value ::extra extra)))
    (catch Throwable ex
      ;; TODO: is there a better way to report nREPL middleware failures?
      (throw (ex-info (str "Visualization failed:" (ex-message ex))
                      {:id ::extra-calculation-failed}
                      ex)))))

(defn eval-with-extra
  "Returns a wrapping map with extra information as value"
  [form]
  (let [value (clojure.core/eval form)]
    (if (instance? IMeta value)
      (with-extra form value)
      value)))

(defn unwrap-value
  "Collapses extra fields from value into msg if present"
  [{:as msg :keys [value]}]
  (if-let [extra (some-> value meta ::extra)]
    (-> (merge msg extra)
        (update :value dissoc-meta ::extra))
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
    :eval `eval-with-extra
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
