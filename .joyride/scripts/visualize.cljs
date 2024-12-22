(ns visualize
  (:require ["path" :as path]
            ["vscode" :as vscode]
            [promesa.core :as p]
            [joyride.core :as joy]
            ["ext://betterthantomorrow.joyride" :as joy-api]
            ["ext://betterthantomorrow.calva$v1" :as calva]
            [clojure.edn :as edn]))

(defonce kindly-view*
  (atom nil))

(defn open-view!
  "Returns the view if it exists, otherwise creates one"
  []
  (or @kindly-view*
      (doto (vscode/window.createWebviewPanel "kindly-render"
                                              "Kindly render"
                                              vscode/ViewColumn.One
                                              #js{:enableScripts true})
        (.onDidDispose #(reset! kindly-view* nil))
        (->> (reset! kindly-view*)))))

(defn show!
  "Sets the html of the view"
  [{:keys [kindly/kind kindly-render/html]}]
  (when-let [view (open-view!)]
    ;; bring it to the top if it is hidden
    (.reveal view)
    ;; set the html
    (set! (-> view .-title) (str "Kindly " (name (or kind "html"))))
    (set! (-> view .-webview .-html) html)))

;; TODO: this shouldn't be necessary in the future
(calva/repl.evaluateCode "clj" "(set! *print-meta* true)")

;; can all kindly objects?
;; (pr-str) (edn/read-string)

;; BufferedImage
;;  * not serialized by default
;; Dataset???
;;  * can be huge
;;  * visualization is small (just a summary)
;;  * summarization needs to happen on the JVM
;;  * idea: maybe Dataset serialization should include the summary
;; Middleware allows you to specify a specific printer to use
;; Calva can also do customization of printing

;; Not all REPLs support middleware, but we want them to get the same experience
;; ClojureScript should be a first class citizen for features for Calva



(defn visualize! [code]
  (-> (p/let [evaluation (calva/repl.evaluateCode "clj" code)]
        ;; TODO: get the visualization as extra info
        (show! {:kindly-render/html (:html (:scicloj.kindly-render.nrepl.kindly-render-middleware/extra
                                            (meta (edn/read-string (.-result evaluation)))))
                :kindly/kind :kind/eval}))
      (p/catch (fn [e]
                 (println "Evaluation error:" e)))))

#_(visualize! "^:kind/hiccup [:h1 \"hello\"]")

;; TODO: Clay vscode commands???
;; Custom commands are code, but the code runs on server REPL process
;; visualize runs in client (ClojureScript) process
;; its a command that needs to use the result

;; Joyride is a way without an extension,
;; with an extension

;; Problem: Custom commands run code in REPL server
;; -- Calva API might help?
;; -- can we get the current form?

#_(visualize! "(do (require '[scicloj.kindly.v4.kind :as kind])
                 (kind/echarts {:title   {:text \"Echarts Example\"}
                                :tooltip {}
                                :legend  {:data [\"sales\"]}
                                :xAxis   {:data [\"Shirts\", \"Cardigans\", \"Chiffons\", \"Pants\", \"Heels\", \"Socks\"]}
                                :yAxis   {}
                                :series  [{:name \"sales\"
                                           :type \"bar\"
                                           :data [5 20 36 10 10 20]}]}))")

(prn "CURRENTFORM" (second (calva/ranges.currentForm)))

(visualize! (second (calva/ranges.currentForm)))

(defn show-file! [filename]
  (p/let [uri (vscode/Uri.file (path/join vscode/workspace.rootPath filename))
          data (vscode/workspace.fs.readFile uri)
          html (.decode (js/TextDecoder. "utf-8") data)]
    (show! {:kindly/kind filename
            :kindly-render/html html})))

;; This file was written by the nREPL middleware
(comment
  (show-file! "test.html")
  )

(comment
  (show! {:kindly/kind :kind/greeting
          :kindly-render/html "<h1>Hello world</h1>"})

  (show! {:kindly/kind :kind/hiccup
          :kindly-render/html "hiccup"}))


(comment
  ;; TODO: vscode has a proposed feature that can be activated
  ;; set package.json#enabledApiProposals-property [editorInsets]
  (vscode/window.createWebviewTextEditorInset "kindly-render"
                                              "kindly-render"
                                              vscode.ViewColumn.One
                                              {:enablesScripts true}))

(comment
  ;; ways to send output
  (println "hello REPL")
  (vscode/window.showInformationMessage "this is a message")
  (defn output [msg]
    (.appendLine (joy/output-channel) msg))
  (output "this is some output")
  (js/console.log "this is some console"))


(comment
  ;; TODO: doesn't work, probably not required anyway
  (def joyrideExt
    (vscode/extensions.getExtension "betterthantomorrow.joyride"))
  (def joyApi
    (.-exports joyrideExt))
  (def subscriptions (.getContextValue joyApi "subscriptions"))
  (.push subscriptions
         (vscode/commands.registerCommand "show"
                                          (fn [] (output "show")))))