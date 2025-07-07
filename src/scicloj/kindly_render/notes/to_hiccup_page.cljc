(ns scicloj.kindly-render.notes.to-hiccup-page
  (:require [scicloj.kindly-render.entry.hiccups :as hiccups]))

(def hiccup-keys
  [:comment-hiccup
   :code-hiccup
   :out-hiccup
   :err-hiccup
   :hiccup
   :ex-hiccup])

(defn check-hiccup
  "Checks that hiccup can be rendered and returns it.
  This allows exceptions to capture the note which will be helpful for debugging."
  [note k h]
  (try #?(:clj (hiccup.core/html h))
       h
       (catch #?(:clj Exception :cljs :default) ex
         (throw (ex-info (str "bad " k ": " h)
                         {:id     ::bad-hiccup
                          :k      k
                          :hiccup h
                          :note   note}
                         ex)))))

(defn hiccups
  "Returns all hiccups present in the notebook"
  [{:as notebook :keys [notes]}]
  (for [note notes
        k hiccup-keys
        :let [h (get note k)]
        :when h]
    (check-hiccup note k h)))

(defn page
  "Returns a hiccup representation of a page"
  [{:as notebook :keys [resource-hiccups]}]
  (let [{:keys [head body]} resource-hiccups]
    (list [:head head]
          [:body
           [:div.container (hiccups notebook)]
           body])))

(defn render-notebook
  "Returns an edn string representation of a notebook as a hiccup page"
  [notebook]
  (-> (hiccups/with-hiccups notebook)
      (page)
      (pr-str)))
