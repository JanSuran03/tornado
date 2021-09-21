(ns tornado.at-rules
  "Current available at-rules: @media, @font-face, @keyframes"
  (:require [tornado.types :as t])
  #?(:clj (:import (tornado.types CSSAtRule))))

(defn cssatrule [id val]
  #?(:clj  (CSSAtRule. id val)
     :cljs (t/CSSAtRule. id val)))

(defn cssatrule? [x]
  (instance? #?(:clj  CSSAtRule
                :cljs t/CSSAtRule) x))

(defn at-media
  "Takes a rules map and any number of media changes and creates a CSSAtRule instance
  with \"media\" identifier:

  (at-media {:screen    :only
             :max-width (u/px 600)
             :min-width (u/px 800}
             [:& {:margin [[(u/px 15 0 (u/px 15) (u/px 20]]
             [:.abc #:def {:margin  (u/px 20)
                           :padding [[(u/px 30) (u/px 15)]]
               [:span {:background-color (colors/mix :red :green)]]
             [:footer {:font-size (u/em 1)])

  The :& selector selects the current element.
  As you can see, you can nest the affected CSS hiccup how you only want.
  Special rules values: :screen :only => only screen
                        :screen true  => screen
                        :screen false => not screen

  {:screen    true
   :speech    false
   :max-width (u/px 600)
   :min-width (u/px 800}
   => @media screen and not speech and (min-width: 600px) and (max-width: 800px) {..."
  [rules & changes]
  (cssatrule "media" {:rules   rules
                      :changes changes}))

(defn at-font-face
  "Can be used for more convenient describing of @font-face. This is how example
  props-maps look like:

  {:src         [[(f/url \"../webfonts/woff2/roboto.woff2\") (f/css-format :woff2)]
                 [(f/url \"../webfonts/woff/roboto.woff\") (f/css-format :woff)]])
   :font-family \"Roboto\"
   :font-weight :normal
   :font-style  :italic}

  This function can receive any number of props maps so that you can also write
  the example above this way:

  {:src         [[(f/url \"../webfonts/woff2/roboto.woff2\") (f/css-format :woff2)]]}
  {:src         [[(f/url \"../webfonts/woff/roboto.woff\") (f/css-format :woff)]]
   :font-family \"Roboto\"
   :font-weight :normal
   :font-style  :italic}"
  [& props-maps]
  (cssatrule "font-face" props-maps))

(defmacro defkeyframes
  "Defines a CSS @keyframes animation. The animation name should have a unique symbol
  for later reference to it and then animation frames in a format [progress params]:

  (defkeyframes fade-in-opacity
                [(u/percent 0) {:opacity 0}]
                [(u/percent 25) {:opacity 0.1}]
                [(u/percent 50) {:opacity 0.25}]
                [(u/percent 75) {:opacity 0.5}]
                [(u/percent 100) {:opacity 1}])

  Then, refer it the CSS hiccup list to make tornado compile it for later usage:

  (def styles
     (list
        fade-in-opacity
        ...))

  After that, you can assign this animation to whatever element you want:

  (def styles
     (list
        fade-in-opacity
        [:.some-element {:animation-duration (u/ms 500)
                         :animation-name     fade-in-opacity)}]
        [:#another-element {:animation-name  fade-in-opacity
                            :animation-delay (u/s 1.5)}]))

  With defkeyframes you can also define from & to progress animations:

  (defkeyframes translate-animation
                [:from {:transform (f/translate (u/px 100) (u/px 200)}]
                [:to {:transform (f/translate (u/px 200) (u/px 400)}])"
  [animation-name & frames]
  `(def ~animation-name (cssatrule "keyframes" {:anim-name (str '~animation-name)
                                                :frames    (list ~@frames)})))

(defn at-media?
  "Returns true if the expression is a CSSAtRule instance with \"media\" identifier."
  [expr] (and (cssatrule? expr)
              (= (:identifier expr) "media")))

(defn at-font-face?
  "Returns true if the expression is a CSSAtRule instance with \"font-face\" identifier."
  [expr] (and (cssatrule? expr)
              (= (:identifier expr) "font-face")))

(defn at-keyframes?
  "Returns true if the expression is a CSSAtRule instance with \"keyframes\" identifier."
  [expr] (and (cssatrule? expr)
              (= (:identifier expr) "keyframes")))