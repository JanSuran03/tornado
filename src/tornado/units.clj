(ns tornado.units
  (:require [tornado.types])
  (:import (tornado.types CSSAtRule CSSUnit)))

(comment
  (defmacro defkeyframes
    [animation-name & frames]
    `(def ~animation-name (CSSAtRule. "keyframes" {:anim-name (str '~animation-name)
                                                   :frames    (list ~@frames)}))))

(defmacro defunit
  ([unit]
   (let [compiles-to (str unit)]
     `(defunit ~unit ~compiles-to)))
  ([identifier css-unit]
   `(def ~identifier (fn [value#] (CSSUnit. ~css-unit value#)))))