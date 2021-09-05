(ns tornado.stylesheet
  (:require [tornado.types])
  (:import (tornado.types CSSUnit CSSFunction CSSAtRule CSSColor)))

(defn at-media [rules & changes]
  (CSSAtRule. "media" {:rules   rules
                       :changes changes}))

(defn at-font-face
  "(at-font-face {:font-family \"Source Sans Pro\"
                  :src [\"url1
                        \"url2
                        ...]
                  :font-weight :bold})"
  [props-map]
  (CSSAtRule. "font-face" props-map))

(defn at-keyframes [name anim-class-or-ids props]
  (CSSAtRule. "keyframes" nil))