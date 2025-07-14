(ns scicloj.kindly-render.notes.to-markdown-page
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [hiccup.core :as hiccup]
            [scicloj.kindly-render.entry.markdowns :as markdowns]))

(def md-keys
  [:comment-md
   :code-md
   :out-md
   :err-md
   :global-out-md
   :global-err-md
   :md
   :ex-md])

(defn page [{:as notebook :keys [resource-hiccups notes kindly/options]}]
  (let [{:keys [head body]} resource-hiccups
        {:keys [front-matter]} options
        markdowns (for [note notes
                        k md-keys
                        :let [md (get note k)]
                        :when md]
                    md)]
    (str
      (when front-matter
        (str "---" \newline
             (str/trim-newline (json/write-str front-matter)) \newline
             "---" \newline \newline))
      ;; TODO: for a book these should just go in _quarto.yml as include-in-header

      (when (seq head)
        (str (str/join \newline (map #(hiccup/html %) head))
             \newline \newline))
      (str/join (str \newline \newline) markdowns) \newline
      (when (seq body)
        (str \newline
             (str/join \newline (map #(hiccup/html %) body))
             \newline)))))

(defn render-notebook
  "Given a notebook, returns a markdown page as a string"
  [notebook]
  (-> (markdowns/with-markdowns notebook)
      (page)))
