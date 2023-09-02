# Tornado

A Clojure library designed to generate CSS using [hiccup-like](https://github.com/weavejester/hiccup)
data structures with focus on simplicity.

## Changelog
is available [here](CHANGELOG.md)

[![Build](https://github.com/JanSuran03/tornado/actions/workflows/clojure.yml/badge.svg)](https://github.com/JanSuran03/tornado/actions/workflows/clojure.yml)

## Benefits
- simple description as data - vectors for selectors, maps for styles, like in Hiccup or Garden
- works in both Clojure and ClojureScript
- everything can be found in a single namespace and there aren't any collisions, you can just `:refer :all`

## Clojars builds

[![Clojars Project](http://clojars.org/org.clojars.jansuran03/tornado/latest-version.svg)](http://clojars.org/org.clojars.jansuran03/tornado)

### [Lein-tornado](https://github.com/JanSuran03/lein-tornado)

A hot-code reloading plugin for automatic compilation of Tornado stylesheets:

[![Clojars Project](http://clojars.org/org.clojars.jansuran03/lein-tornado/latest-version.svg)](http://clojars.org/org.clojars.jansuran03/lein-tornado)


If you are familiar with [garden](https://github.com/noprompt/garden), you should not have any problems with switching to Tornado.

Require a namespace ***tornado.core***, where you have available ***everything useful in this library***. Define some example CSS-hiccup like below and
run it with a function "repl-css" to see the compiled CSS:

```clojure
(require '[tornado.core :refer :all])
=> nil

(def styles
  [:#some-id {:width            (px 15)
              :color            :font-black
              :padding          (join 5 10 15 20)
              :gap              (join em 4 2)
              :background-color (rgb 100 150 200)}])
=> #'user/styles

(repl-css styles)

#some-id {
    width: 15px;
    color: #1A1B1F;
    padding: 5px 10px 15px 20px;
    gap: 4em 2em;
    background-color: rgb(100, 150, 200);
}

=> nil
```

For compiling **and saving** the stylesheet, there is a function tornado.compiler/css, also referred in tornado.core:

```clojure
;; First, you have to ensure that the path "resources/css" exists. In the plugin
;; Lein-tornado, this is not a problem anymore, the library creates the folders for you.
(css {:output-to "resources/css/example.css"} styles)
=> nil
```

ClojureScript:

```clojure
(ns example.core
  (:require [reagent.dom :as r-dom]
    [tornado.core :as t :refer [em]]) ; in ClojureScript, you cannot refer the whole namespace tornado.core
  (:require-macros [tornado.macros :refer [defunit]])) ; note that you cannot refer macros from tornado.core, like in Clojure

(defunit vw) ; (viewport width) compiles to "vw", string form of the reference function
(defunit celsius "°C") ; `celsius` is the reference, compiles to "°C"

;; Reagent does not support fully compilated params of every kind, but you can precompile :style.
(defn root []
  [:h2 {:style (compile-params {:color           :chocolate
                                :font-size       (em 15)
                                :position        :relative
                                :left            (vw 25)
                                :air-temperature (celsius 22)})}
   "Abc"])

(defn render []
  (r-dom/render
    [root]
    (.getElementById js/document "app"))
  (js/console.log "Hello World!"))
```

### The complete documentation with examples will be under this link: https://orgpad.com/s/SjH_TDbx4PH

## Plans for the future:

- Docs to CSS functions, better docs overall.
- More common utility functions (currently only 3).
- More at-rules functions (currently @media, @font-face, @keyframes).
- The code could be simpler on some places.
- More examples in the OrgPad document.

## Contributions
- are welcome

## Contact

Although there are more ways to contact me, you can e-mail me to **honzik (dot) suran (at) seznam (dot) cz**. I will
always try to reply ASAP. I am also on discord: **Honza_Suran#6703**

## License

Copyright © 2021 Jan Šuráň

Distributed under the [Eclipse Public License](#http://www.eclipse.org/legal/epl-2.0).
