(ns tornado.units
  (:require [tornado.types])
  (:import (tornado.types CSSUnit)))

(defmacro defunit
  ([unit]
   (let [compiles-to (str unit)]
     `(defunit ~unit ~compiles-to)))
  ([identifier css-unit]
   `(def ~identifier (fn [value#] (CSSUnit. ~css-unit value#)))))