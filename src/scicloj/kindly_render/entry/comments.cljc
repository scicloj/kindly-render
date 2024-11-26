(ns scicloj.kindly-render.entry.comments
  (:require [clojure.string :as str]))

(defn inline-markdown
  "Strips comment indicators and separates headers"
  [code]
  (->> (str/split-lines code)
       #! clojure comments can be shebang style, useful for babashka
       (map #(str/replace-first % "#!" "    #!"))
       ;; inline comments remove the semicolons
       (map #(str/replace-first % #"^(;+)\s?" ""))
       ;; keep titles separated in markdown
       (map #(str/replace-first % #"^#" "\n#"))
       (str/join \newline)))
