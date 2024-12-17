# kindly-render

![Kindly logo](Kindly.svg)

`kindly-render` is a Clojure library for rendering kinds as markdown or html.

[Clay](https://github.com/scicloj/clay) is for literate and interactive programming.
This library was extracted from Clay to be more useful to other visual tool developers.
The hope is to make it convenient to share visualizations between tools.

## Related projects

[Kindly](https://scicloj.github.io/kindly-noted/kindly) is a common ground for defining how things should be visualized, seeking compatibility across tools.

[kindly-advice](https://scicloj.github.io/kindly-noted/kindly_advice) is a library that helps tools such as Clay to be Kindly-compatible.

[read-kinds](https://github.com/scicloj/read-kinds) is used internally by Claykind (and soon by Clay as well) to generate Kindly advice from notebooks expressed as Clojure namespaces.

[Claykind](https://github.com/timothypratley/claykind) is rethinking the Clay architecture and implementation from scratch, in a new code base. The two projects are being developed in coordination.

## Rationale

Visual toolmakers may benefit from having a shared collection of viewers for the kinds defined by Kindly.
Rendering visualizations should be possible in both Clojure and ClojureScript.
Viewers may need to be tailored to different usage scenarios.

## What are notes?

The purpose of this library is to take a `note` or `notebook` consisting of `notes`,
and produce markdown, hiccup, or html for displaying them.
A note is a map containing either a value, form, or both.

Example note:

```clojure
{:form (+ 1 2)
 :value 3}
```

Example notebook:

```clojure
{:notes [{:value 3} {:value "Hello world"}]}
```

A note may be annotated for visualization with kindly:

```clojure
{:value (kind/hiccup [:svg [:circle {:r 10}]])}
```

## Usage

[![Clojars Project](https://img.shields.io/clojars/v/org.scicloj/kindly-render.svg)](https://clojars.org/org.scicloj/kindly-render)

```clojure
(require '[scicloj.kindly-render.note.to-hiccup :as to-hiccup])
(def my-note {:value {:my "value"}})
(to-hiccup/render my-note)
```

Returns a hiccup representation of the visualization of `my-note`.

```clojure
(require '[scicloj.kindly-render.notes.to-html-page :as to-html-page])
(def my-notebook {:notes [my-note]})
(to-html-page/notes-to-html my-notebook)
```

Returns a string of HTML representation of the `my-notebook`.

See [examples/basic/page.clj](examples/basic/page.clj) for a concrete example.

## Design

Sometimes we want to produce a JavaScript enabled visualization, but sometimes we can't. Maybe we are making a PDF, and so we need to use a static image instead.

Under `scicloj.kindly-render.note` are several targets:

* `to-hiccup` (plain)
* `to-hiccup-js` (javascript enhanced)
* `to-markdown`
* `to-scittle-reagent`

The main entry point for each is `render`.
When making a PDF from HTML, users would call `to-hiccup/render` and get an image, but when making a rich web page they might call `to-hiccup-js/render` and get the JavaScript enhanced HTML instead.

`render` calls `kindly-advice/advise` which ensures that the input `note` has a completed value.
Completion means that if `note` only contained a `:form`, that form would be evaluated to produce a value and added as `:value`.
Furthermore, both the form and value are checked for kindly annotations as metadata, or the kind may be inferred.
At this point the `note` will contain a key `:kind` which indicates the visualization requested (if any).

The completed note is passed to `render-advice` which is a multimethod that dispatches on the `:kind` to produce the target output.

### A multi-method per target

Each target has a multi-method called `render-advice` with methods defined for as many `:kinds` as are supported by that target.

Using multi-methods allows users to use the standard features of Clojure to add or replace viewers if they would like to.

#### Fallback

Each multi-method has a `:default` implementation.
If no viewer is matched in `markdown`, it will fall back to `hiccup-js`.
If no viewer is matched there, then it will fall back to `hiccup`.
Fallback only happens in one direction because `hiccup` cannot fall back to `markdown`, but `markdown` can fall back to `hiccup`.
Markdown is not valid HTML, however it should be noted that hiccup may contain markdown values like `(kind/md "# this is markdown")`,
these will be converted to HTML by the hiccup renderer.
Similarly plain `hiccup` cannot fall back to `hiccup-js`, but hiccup-js can fall back to `hiccup`.

### Nested visualizations

Sometimes visualizations may contain other visualizations.
For example, we may wish to present a vector containing a chart and a table.
Data structures may contain nest visualizations,
and these in turn may contain further nesting.

Nesting kinds are: `:kind/vector`, `:kind/map`, `:kind/set`, `:kind/seq`, `:kind/hiccup` and `:kind/table`.

Each target multi-method must have a method for each nesting kind that calls `walk/render-hiccup-recursively`.

### Hiccup

Hiccup is a special data structure that requires a little more care.
Other visualizations may be nested inside the hiccup.

Each target multi-method must have a method for `:kind/hiccup` for recursively rendering that calls `walk/render-hiccup-recursively`.

### Optional dependencies

`kind-portal` is excluded by default. In order to use Portal you will need to add it to your
`deps.edn` file.

## Discussion

Regular updates are given at the [visual-tools meetings](https://scicloj.github.io/docs/community/groups/visual-tools/).

The best places to discuss this project are:
* a topic thread under the [#kindly-dev stream](https://clojurians.zulipchat.com/#narrow/channel/454856-kindly-dev) at the Clojurians Zulip (more about chat streams [here](https://scicloj.github.io/docs/community/chat/))
* a [github issue](https://github.com/scicloj/kindly-noted/issues)
* a thread at the [visual-tools channel](https://clojurians.slack.com/archives/C02V9TL2G3V) of the Clojurians slack

## License

Copyright © 2024 Scicloj

[Eclipse Public License - v 2.0](LICENSE)
