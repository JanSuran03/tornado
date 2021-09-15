# Tornado

A Clojure library designed to generate CSS using [hiccup-like](https://github.com/weavejester/hiccup)
data structures with focus on simplicity.

Every feature in this library should work properly, but it is still very new, there are not all features that CSS
offers (but very most of them for common usage are included). As the time passes, more new features will be added.

The Tornado library is not designed to work in ClojureScript.

## Usage

If you are familiar with [garden](https://github.com/noprompt/garden), you should not have any problems with switching to Tornado.

First, you have to create a list of hiccup-like CSS structure that will be compiled. Require a namespace ***tornado.core***, where you
have available ***everything useful in this library***. Define some example CSS like below and run it with a function "repl-css":

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

### *The complete documentation with examples will be under this link: https://orgpad.com/s/SjH_TDbx4PH*

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