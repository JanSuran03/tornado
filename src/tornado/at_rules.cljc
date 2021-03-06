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
             :max-width (px 600)
             :min-width (px 800)}
             [:& {:margin (join 15 0 15 20)}]
             [:.abc #:def {:margin  (px 20)
                           :padding (join 30 15)}
               [:span {:background-color (mix-colors :red :green)}]]
             [:footer {:font-size (em 1)}])

  The :& selector selects the current element.
  As you can see, you can nest the affected CSS hiccup how you only want.
  Special rules values: :screen :only => only screen
                   :screen true  => screen
                   :screen false => not screen

  {:screen    true
   :speech    false
   :max-width (px 600)
   :min-width (px 800)}
   => @media screen and not speech and (min-width: 600px) and (max-width: 800px) {...}"
  [rules & changes]
  (cssatrule "media" {:rules   rules
                      :changes changes}))

(defn at-font-face
  "Can be used for more convenient describing of @font-face. This is how example
  props-maps look like:

  {:src         [[(f/url \"../webfonts/woff2/roboto.woff2\") (f/css-format :woff2)]
                 [(f/url \"../webfonts/woff/roboto.woff\") (f/css-format :woff)]]
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