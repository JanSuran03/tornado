(ns tornado.at-rules
  (:require [tornado.types])
  (:import (tornado.types CSSAtRule)))

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
   => @media screen and not speech and (min-width: 600px) and (max-width: 600px) {..."
  [rules & changes]
  (CSSAtRule. "media" {:rules   rules
                       :changes changes}))

(defn at-font-face
  "(at-font-face {:font-family \"Source Sans Pro\"
                  :src [\"url1
                        \"url2
                        ...]
                  :font-weight :bold})"
  [& props-maps]
  (CSSAtRule. "font-face" props-maps))

(defmacro defkeyframes
  "Doc"
  [animation-name & frames]
  `(def ~animation-name (CSSAtRule. "keyframes" {:anim-name (str '~animation-name)
                                                 :frames    (list ~@frames)})))

(defn at-media?
  "Returns true if the expression is a CSSAtRule instance with \"media\" identifier."
  [expr] (and (instance? CSSAtRule expr)
              (= (:identifier expr) "media")))

(defn at-font-face?
  "Returns true if the expression is a CSSAtRule instance with \"font-face\" identifier."
  [expr] (and (instance? CSSAtRule expr)
              (= (:identifier expr) "font-face")))

(defn at-keyframes?
  "Returns true if the expression is a CSSAtRule instance with \"keyframes\" identifier."
  [expr] (and (instance? CSSAtRule expr)
              (= (:identifier expr) "keyframes")))