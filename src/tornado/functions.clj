(ns tornado.functions
  (:require [tornado.types]
            [tornado.compiler :refer [general-parser-fn compile-expression]]
            [tornado.util :as util])
  (:import (tornado.types CSSFunction)
           (clojure.lang PersistentList)))

(defn- make-cssfn-record
  "An internal CSSFunction function which takes the \"compiles-to\"
  function parameter, e.g. min\", a function which is applied to args
  during the compilation and the arguments and creates a CSSFunction record."
  [compiles-to* compile-fn* & args]
  (CSSFunction. compiles-to* compile-fn* args))

(defmacro defcssfn
  "Defines a CSS function:

  Arity (1):
     (defcssfn translate)
     => #'tornado.functions/translate
     (translate 10 20 30)
     => #tornado.types.CSSFunction{:compiles-to \"translate\"
                                   :compile-fn  #'tornado.util/general-parser-fn
                                   :args        (10 20 30)}
     (let [{:keys [compile-fn] :as my-fn} *1]
       (compile-fn my-fn))
     => \"translate(15, 20, 30)\"

  Arity (2):
     (defcssfn min* \"min\")
     => #'tornado.functions/min*
     (min* \"50px\" \"4vw\")
     => #tornado.types.CSSFunction{:compiles-to \"min\"
                                   :compile-fn  #'tornado.util/general-parser-fn
                                   :args        (\"50px\", \"4vw\")}
     (let [{:keys [compile-fn] :as my-fn} 1]
       (compile-fn my-fn))
     => \"min(50px, 4vw)\"

     (defcssfn scale (fn [{:keys [args]}]
                       (str \"scale(\" (->> args (map tornado.util/int*)
                                            (str/join \", \"))
                                     \")\")))
     => #'tornado.functions/scale
     (let [{:keys [compile-fn] :as my-fn} (scale 3.0 6/4)]
       (compile-fn my-fn))
     => \"scale(3, 1.5)\"

  With arity (3), if you look at the 2-arity examples, the first arg would be min*,
  the second arg \"min\" and the third arg the function (fn [{:keys [args]}] ...)."
  ([fn-name]
   (let [compiles-to (str fn-name)]
     `(defcssfn ~fn-name ~compiles-to nil)))
  ([fn-name css-fn-or-fn-tail]
   (condp instance? css-fn-or-fn-tail String `(defcssfn ~fn-name ~css-fn-or-fn-tail nil)
                                      PersistentList (let [compiles-to (str fn-name)]
                                                       `(defcssfn ~fn-name ~compiles-to ~css-fn-or-fn-tail))
                                      (throw (IllegalArgumentException.
                                               (str "Error defining a CSS function " fn-name " with arity(2):"
                                                    "\nThe second argument " css-fn-or-fn-tail " is"
                                                    " neither a string nor a function.")))))
  ([clojure-fn-name compiles-to compile-fn]
   `(def ~clojure-fn-name (partial ~make-cssfn-record ~compiles-to ~compile-fn))))

(defcssfn translate)
(defcssfn translate3d)
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
(defcssfn rotate3d)
(defcssfn cubic-bezier)
(defcssfn calc (fn [{:keys [args]}]
                 (str "calc(" (->> args (map compile-expression) util/str-spacejoin) ")")))

;; symbols for calc function
(def add "+")
(def sub "-")
(def mul "*")
(def div "/")