# Tornado

A Clojure library designed to generate CSS using [hiccup-like](https://github.com/weavejester/hiccup)
data structures with focus on simplicity.

The library is very new, but I will try to update it depending on how my time works out.

The Tornado library is not designed to work in ClojureScript.

## Usage

[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.jansuran03/tornado.svg)](https://clojars.org/org.clojars.jansuran03/tornado)

### [Lein-tornado](https://github.com/JanSuran03/lein-tornado):

A hot-code reloading plugin for automatic compilation Tornado stylesheets:

[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.jansuran03/lein-tornado.svg)](https://clojars.org/org.clojars.jansuran03/lein-tornado)


If you are familiar with [garden](https://github.com/noprompt/garden), you should not have any problems with switching to Tornado.

Require a namespace ***tornado.core***, where you have available ***everything useful in this library***. Define some example CSS like below and run it with a function "repl-css":

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

For compiling and saving the stylesheet, there is a function tornado.compiler/css, also referred in tornado.core:

```clojure
;; First, you have to ensure that the path "resources/css" exists. This will be solved later.
(css {:output-to "resources/css/example.css"} styles)
=> nil
```

There is not any better compilation management **yet**, but there will be soon.

### The complete documentation with examples will be under this link: https://orgpad.com/s/SjH_TDbx4PH

# Plans for the future

Since this library already has a lot of features, but it is not too practical for a project where you should recompile
the hiccup every few seconds, I will soon be working on a hot-code reloading plugin, similar to [lein-garden](https://github.com/noprompt/lein-garden).
More detailed and better documentations are coming later. Everything takes time, especially when I want the documentations
and the whole library to be simple, clear, flexible and comfortable.

## Contact

Although there are more ways to contact me, you can send me an e-mail to **suran (dot) orgpad (at) gmail (dot) com**. I will
always try to reply as soon as possible. I am also on discord: **Honza_Suran#6703**

## License

Copyright © 2021 Jan Šuráň

Distributed under the [Eclipse Public License](#http://www.eclipse.org/legal/epl-2.0.)