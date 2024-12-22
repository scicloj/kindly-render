(ns scicloj.kindly-render.nrepl.kindly-render-middleware
  "nREPL middleware for adding visualization information to values"
  (:require [nrepl.core :as nrepl]
            [nrepl.middleware :as middleware]
            [nrepl.transport :as t]
            [nrepl.middleware.interruptible-eval :as eval]
            [nrepl.middleware.print :as print]
            [scicloj.kindly.v4.kind :as kind])
  (:import (nrepl.transport Transport)
           (clojure.lang IMeta IReference)))

(kind/md "Presenting the results of evaluation")

(defn keep-meta
  "Observes meta and form-meta, regardless of the state of *print-meta*"
  [{:keys [form value]}]
  {:meta      (meta value)
   :form-meta (meta form)})

(defonce ^{:doc "A function that will be called with `{:form form, :value value}` and returns a map of extra information for IDE use"}
  visualize
  keep-meta)

(defn install!
  "The visualizer could be a pluggable, configurable part"
  [sym]
  (alter-var-root #'visualize (constantly (requiring-resolve sym))))

;; TODO: find a nicer way to facilitate installation of visualizer(s)
;; the current method is here for testing convenience
#_(install! 'keep-meta)
(install! 'scicloj.kindly-render.nrepl.clay-middleware/clay-render)
#_(install! 'scicloj.kindly-render.notes.to-html-page/render-note)

(defn update-meta [value f & args]
  "Does alter-meta!, vary-meta, or nothing as appropriate for value"
  (cond (instance? IReference value) (do (apply alter-meta! value f args) value)
        (instance? IMeta value) (apply vary-meta value f args)
        :else value))

(defn with-extra
  "Adds extra information to be added when the transport sends value from server to client.
  Only operates on values that can have metadata."
  [form value]
  (try
    (let [note {:form  form
                :value value}
          extra (visualize note)]
      (if (:html extra)
        ;; TODO: Cursive should use html in msg instead of tagged literals
        (tagged-literal 'cursive/html (select-keys extra [:html]))
        (update-meta value assoc ::extra extra)))
    (catch Throwable ex
      ;; TODO: is there a better way to report nREPL middleware failures?
      (throw (ex-info (str "Visualization failed:" (ex-message ex))
                      {:id ::extra-calculation-failed}
                      ex)))))

(defn eval-with-extra
  "Returns a wrapping map with extra information as value"
  [form]
  (let [value (clojure.core/eval form)]
    (with-extra form value)))

(defn unwrap-value
  "Collapses extra fields from value into msg if present"
  [{:as msg :keys [value]}]
  (if-let [extra (some-> value meta ::extra)]
    (-> (merge msg extra)
        ;; TODO: should remove from value when IDEs have the ability to find the information elsewhere
        ;;(update :value update-meta dissoc ::extra)
        )
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
