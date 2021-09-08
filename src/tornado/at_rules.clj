(ns tornado.at-rules
  (:require [tornado.types])
  (:import (tornado.types CSSAtRule)))

(defn at-media [rules & changes]
  (CSSAtRule. "media" {:rules   rules
                       :changes changes}))

(defn at-media? [expr]
  (and (instance? CSSAtRule expr)
       (= (:identifier expr) "media")))

(defn at-font-face
  "(at-font-face {:font-family \"Source Sans Pro\"
                  :src [\"url1
                        \"url2
                        ...]
                  :font-weight :bold})"
  [props-map]
  (CSSAtRule. "font-face" props-map))

(defmacro defkeyframes
  [animation-name & frames]
  `(def ~animation-name (CSSAtRule. "keyframes" {:anim-name (str '~animation-name)
                                                 :frames    (list ~@frames)})))