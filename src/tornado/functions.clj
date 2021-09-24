(ns tornado.functions
  (:require [tornado.types]
            [tornado.util :as util])
  (:import (tornado.types CSSFunction)))

(defmacro defcssfn
  ([fn-name]
   (let [compiles-to (str fn-name)]
     `(defcssfn ~fn-name ~compiles-to nil)))
  ([fn-name css-fn-or-fn-tail]
   (cond (string? css-fn-or-fn-tail) `(defcssfn ~fn-name ~css-fn-or-fn-tail nil)
         (list? css-fn-or-fn-tail) (let [compiles-to (str fn-name)]
                                     `(defcssfn ~fn-name ~compiles-to ~css-fn-or-fn-tail))
         (ifn? css-fn-or-fn-tail) (let [compiles-to (str fn-name)]
                                    `(defcssfn ~fn-name ~compiles-to ~css-fn-or-fn-tail))
         :else (util/exception
                 (str "Error defining a CSS function " fn-name " with arity(2):"
                      "\nThe second argument " css-fn-or-fn-tail " is"
                      " neither a string nor a function."))))
  ([clojure-fn-name compiles-to compile-fn]
   `(def ~clojure-fn-name (fn [& args#] (CSSFunction. ~compiles-to ~compile-fn args#)))))