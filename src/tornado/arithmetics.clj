(ns tornado.arithmetics
  (:require [tornado.types])
  (:import (tornado.types CSSFunction)))

(defmacro cartesian-product [& seqs]
  (let [seqs (cond->> seqs (every? #(every? sequential? %) seqs) (apply concat))
        w-bindings (map #(vector (gensym) %) seqs)
        binding-syms (map first w-bindings)
        for-bindings (vec (apply concat w-bindings))]
    `(for ~for-bindings (vector ~@binding-syms))))

(defn expand-seqs [coll]
  (mapcat (fn [coll]
            (if (seq? coll)
              (expand-seqs coll)
              (list coll)))
          coll))

(defn make-css-fn
  "Creates a CSS function which accepts any number of arguments and allows us to"
  [function]
  (fn [& args]
    (CSSFunction. function args)))

(defmacro defcssfn
  ([function]
   (let [compiles-to (str function)]
     `(defcssfn ~function ~compiles-to)))
  ([identifier css-function]
   `(def ~identifier (make-css-fn ~css-function))))

(defcssfn translate)
(defcssfn translateX)
(defcssfn translateY)
(defcssfn translateZ)
(defcssfn scale)
(defcssfn min* "min")
(defcssfn max* "max")
(defcssfn rotate)
(defcssfn rotateX)
(defcssfn rotateY)
(defcssfn rotateZ)
(defcssfn cubic-bezier)