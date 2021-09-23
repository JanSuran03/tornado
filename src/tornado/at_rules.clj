(ns tornado.at-rules
  (:require [tornado.types])
  (:import (tornado.types CSSAtRule)))

(defmacro defkeyframes
  [animation-name & frames]
  `(def ~animation-name (CSSAtRule. "keyframes" {:anim-name (str '~animation-name)
                                                 :frames    (list ~@frames)})))