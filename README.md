# Tornado

A Clojure library designed to generate CSS using [hiccup-like](https://github.com/weavejester/hiccup)
data structures with focus on simplicity.

The library is very new, but I will try to update it depending on how my time works out.

There are some limitations for ClojureScript, see below.

## Usage


[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.jansuran03/tornado.svg)](https://clojars.org/org.clojars.jansuran03/tornado)


### [Lein-tornado](https://github.com/JanSuran03/lein-tornado)

A hot-code reloading plugin for automatic compilation of Tornado stylesheets:

[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.jansuran03/lein-tornado.svg)](https://clojars.org/org.clojars.jansuran03/lein-tornado)


If you are familiar with [garden](https://github.com/noprompt/garden), you should not have any problems with switching to Tornado.

Require a namespace ***tornado.core***, where you have available ***everything useful in this library***. Define some example CSS-hiccup like below and
run it with a function "repl-css" to see the compiled CSS:

```clojure
(require '[tornado.core :refer :all])
=> nil

(def styles
  [:#some-id {:width            (px 15)
              :color            :font-black
              :background-color (rgb 100 150 200)}])
=> #'user/styles

(repl-css styles)

#some-id {
    width: 15px;
    color: #1A1B1F;
    background-color: rgb(100, 150, 200);
}

=> nil
```

For compiling **and saving** the stylesheet, there is a function tornado.compiler/css, also referred in tornado.core:

```clojure
;; First, you have to ensure that the path "resources/css" exists. In the plugin
;;  Lein-tornado, this is not a problem anymore, the library creates the folders for you.
(css {:output-to "resources/css/example.css"} styles)
=> nil
```

ClojureScript:

```clojure
(ns example.core
  (:require [reagent.dom :as r-dom]
    [tornado.core :as t :refer [em]]) ; in ClojureScript, you cannot refer the whole namespace tornado.core
  (:require-macros [tornado.macros :refer [defunit]])) ; note that you cannot refer macros from tornado.core, like in Clojure

;; is defined in tornado.core, but I want to redefine it as an example
(defunit viewport-height "vh")   ; compiles to "vh"

;; viewport width, compiles to "vw", string form of the unit function
(defunit vw)

(def cex t/compile-expression)

(defn root []
  [:h2 {:style {:color     (cex :chocolate)
                :font-size (cex (em 15))
                :position  (cex :position/absolute)
                :top       (cex (viewport-height -60))
                :left      (cex (vw 25))}}
   "Abc"])

(defn render []
  (r-dom/render
    [root]
    (.getElementById js/document "app"))
  (js/console.log "Hello World!"))
```

### The complete documentation with examples will be under this link: https://orgpad.com/s/SjH_TDbx4PH

## Plans for the future:

- Rewrite java-only functions on a few places to cljc.
- Add html <... style="..."/> data generator.
- Docs to CSS functions, better docs overall.
- More common utility functions (currently only 'important' and 'grid-areas'.
- More at-rules functions (currently @media, @font-face, @keyframes).
- The code could be simpler on some places.
- More examples in the OrgPad document.

## Contact

Although there are more ways to contact me, you can send me an e-mail to **suran (dot) orgpad (at) gmail (dot) com**. I will
always try to reply as soon as possible. I am also on discord: **Honza_Suran#6703**

## License

Copyright © 2021 Jan Šuráň

Distributed under the [Eclipse Public License](#http://www.eclipse.org/legal/epl-2.0).
