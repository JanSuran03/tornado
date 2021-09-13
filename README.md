# Tornado

A Clojure library designed to generate CSS using [hiccup-like](https://github.com/weavejester/hiccup)
data structures with focus on simplicity.

Every feature in this library should work properly, but it is still very new, there is not every features that CSS
offers but as the time passes, more new features will be added.

The Tornado library is not designed to work in ClojureScript.

## Usage

If you are familiar with [garden](https://github.com/noprompt/garden), this library has similar syntax.

First, you have to define a list of hiccup vectors that should be compiled. Refer [repl-css] from tornado.compiler which
pretty-prints the compiled-css in REPL. Define some example CSS like below and run (repl-css ~your-stylesheet~):

```clojure
(require '[tornado.core :refer :all]
         '[tornado.compiler :refer [repl-css]])
=> nil

(def styles
  (list [:#some-id {:width            (px 15)
                    :color            :font-black
                    :background-color (rgb 100 150 200)}]))
=> #'user/styles

(repl-css styles)

#some-id {
    width: 15px;
    color: #1A1B1F;
    background-color: rgb(100, 150, 200);
}

=> nil
```

You can also nest the selectors as you please or even make cartesian product of all given combinations:

```clojure
(-> (list [:#id-1 :#id-2 {:width  (px 500)
                          :height :auto}
           [:.class-1 :.class-2 {:height  (important (percent 50))
                                :display :flex}]])
    repl-css)

#id-2 .class-2, #id-1 .class-1, #id-2 .class-1, #id-1 .class-2 {
    height: 50% !important;
    display: flex;
}

=> nil
```

This is how you can use more advanced selectors (pseudoclass selectors):

```clojure
(-> (list [:#some-id {:padding (px 10)}
           [:.nested-class {:color :black}]
           [hover {:padding [[(px 30) (px 20)]]}]])
    repl-css)

#some-id {
    padding: 10px;
}

#some-id .nested-class {
    color: #000000;
}

#some-id:hover {
    padding: 30px 20px;
}

=> nil
```

Here is an example usage of @media:

```clojure
(-> (list [:#some-id {:padding (vw 20)}
           [:.some-class {:margin (pt 5)}
            (at-media {:screen false
                       :max-width (px 500)}
                      [:& {:margin 0}]
                      [:.some-child {:color (hsl 120 0.5 0.8)}])]])
    repl-css)

#some-id {
    padding: 20vw;
}

@media not screen and (max-width: 500px) {
    #some-id .some-class {
        margin: 0;
    }

    #some-id .some-class .some-child {
        color: hsl(120, 50%, 80%);
    }
}

#some-id .some-class {
    margin: 5pt;
}
```

```clojure
(-> (list (at-font-face {:src         [[(url "webfonts/woff2/roboto.woff2") (css-format :woff2)]]}
                        {:src         [[(url "webfonts/woff/roboto.woff") (css-format :woff)]]
                         :font-weight :normal})
          [:.somesel {:someparam :someval}])
    repl-css)

@font-face {
    src: url(webfonts/woff2/roboto.woff2) format("woff2");
    src: url(webfonts/woff/roboto.woff) format("woff");
    font-weight: normal;
}

.somesel {
    someparam: someval;
}
```

## Contact

Although there are more ways to contact me, for now, suran (dot) orgpad (at) gmail (dot) com is the easiest way. I will
always try to reply as soon as possible.

## License

Copyright © 2021 Jan Šuráň

Distributed under the [Eclipse Public License](#http://www.eclipse.org/legal/epl-2.0.)