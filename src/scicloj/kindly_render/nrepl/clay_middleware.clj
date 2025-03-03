;; TODO: move to the clay project
(ns scicloj.kindly-render.nrepl.clay-middleware
  (:require [scicloj.clay.v2.api :as clay]
            [hiccup.core :as hiccup]))

(defn clay-render [note]
  (when-let [hiccups (seq (clay/make-hiccup {:single-form       (:form note)
                                             :source-path       (:file (meta (:value note)))
                                             :inline-js-and-css true}))]
    {:html (hiccup/html hiccups)}))

(defn wrap [uri]
  (str "<!DOCTYPE html>
<html>
<head>
<style type=\"text/css\">
body, html
{
    margin: 0; padding: 0; height: 100%; overflow: hidden;
}
#content
{
    position:absolute; left: 0; right: 0; bottom: 0; top: 0px;
}
</style>
</head>
<body style=\"margin:0;padding:0;overflow:hidden;\">
  <iframe src=\"" uri "\" style=\"width:100%; height:100%; border:none;\"></iframe>
</body>
</html>"))

(defn clay-inline-uri [form file]
  (clay/make! {:show        true
               :single-form form
               :source-path file})
  (tagged-literal 'cursive/html {:html (wrap (clay/url))}))
