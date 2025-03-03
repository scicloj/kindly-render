(ns visualization-in-editors
  (:require [scicloj.kindly.v4.kind :as kind]
            [tablecloth.api :as tc]
            [scicloj.tableplot.v1.plotly :as plotly]))

;; Notebooks are namespaces that mix *narrative, code, and visualizations*.

;; ## Why visualize?

(def distances
  (tc/dataset [["shop" 0.2]
               ["library" 0.7]
               ["airport" 12]
               #_["DC" 204]
               #_["London" 3461]
               #_["Moon" 338900]]
              {:column-names [:location :distance]}))

(plotly/layer-bar distances
                  {:=y :distance
                   :=x :location})

(kind/echarts {:title   {:text "Echarts Example"}
               :tooltip {}
               :legend  {:data ["sales"]}
               :xAxis   {:data ["Shirts", "Cardigans", "Chiffons", "Pants", "Heels", "Socks"]}
               :yAxis   {}
               :series  [{:name "sales"
                          :type "bar"
                          :data [5 20 36 10 10 20]}]})

(kind/hiccup [:div
              [:h2 "hello world"]
              [:svg [:circle {:r 50}]]])


;; ## Why Kindly?

;; **No rendering dependency in user code**

;; * Uniformity: A single interface for multiple tools like Clay, Portal, and Clerk.
;; * Many visualizations: From simple Markdown to advanced visualizations like Vega or Cytoscape.
;; * Community-Driven: Already powering major documentation projects like Tablecloth and Fastmath.

;; **Already working well for the SciCloj community**

;; ## Not the only way to visualize

;; * Not an inspector (but can request one)

;; can make a command that runs joyride

(kind/portal
 {:a [1 2 3]
  :b [4 5 6]})

;; ## When to use Kindly?

;; Everywhere

;; ## How does Clay compare to Clerk?

;; Clay focuses on being a normal REPL experience that supports visualization and publishing.
;; Clay produces lightweight HTML files.

;; Clerk has more notebook features like cells.
;; Clerk is more dynamic and uses Reagent components.




;; ## What the heck is going on here?

(kind/hiccup [:h2 "And now for something completely different"])

(comment

  (/ 1 0)
  *e

  )


{:calva/flare {:type :webview
               :key :test
               :title "Test flare!"
               :html "<div><h1>I'm HTML</h1><svg><circle r=50></svg></div>"}}

{:calva/flare {:type :info
               :message "Do you like flares?"
               :items ["yes" "no"]
               :then 'visualization-in-editors/selected}}

(defn selected [x]
  {:calva/flare {:type :info
                 :message (str "You selected " x)}})

{:calva/flare {:type :info
               :message "🎉 All tests passed! Great job!"}}

{:calva/flare {:type :webview
               :key :test
               :title "Something for everyone"
               :url "https://calva.io/"}}

{:calva/flare {:type :webview
               :key :test
               :html "A flare is a request for action sent from the REPL server to the REPL client.
                        Flares are one-way, from userspace to IDE.
                        They are value results of user code that are recognized as special values.
                        The REPL client inspects all values it receives, and when it sees a flare, it acts on it.
                        Handling a flare may cause the IDE to send an eval request back to the server.
                        Flares are IDE-specific, as Calva and Cursive may support different features.
                        Flares take the form `{:calva/flare {...}}` where the key indicates the IDE,
                        and the value indicates the action."}}

{:calva/flare {:type :webview
               :key :one-last-thing
               :title "One last thing..."
               :html "flares are generic IDE behaviors, no external dependencies like Clay...
                and Clay can use them without an API"}}

(defn commands [x]
  (def command-result x)
  (prn 'command-count (count command-result)))

{:calva/flare {:type :commands
               :then `commands}}

{:calva/flare {:type :command
               :command "simpleBrowser.api.open"
               :args ["https://calva.io/"]}}

{:calva/flare {:type :command
               :command "vscode.diff"
               :args ["/Users/timothypratley/git/kindly-render/notebooks/visualization_in_editors.clj"
                      "/Users/timothypratley/git/kindly-render/examples/basic/page.clj"
                      "diff title"]}}

(defn cmd [command & args]
  {:calva/flare (cond-> {:type :command
                         :command command}
                  args (assoc :args args))})

(cmd "workbench.action.toggleLightDarkThemes")
(cmd "workbench.action.toggleSidebarVisibility")
(cmd "workbench.action.quickOpen" "app.clj")
(cmd "editor.action.formatDocument")
(cmd "workbench.action.openSettingsJson")
(cmd "simpleBrowser.api.open" "https://calva.io/")

(println command-result)

