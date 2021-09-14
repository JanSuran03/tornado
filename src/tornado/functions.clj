(ns tornado.functions
  "Everything related to CSS functions."
  (:require [tornado.types]
            [tornado.util :as util]
            [tornado.compiler :refer [compile-expression]])
  (:import (tornado.types CSSFunction)
           (clojure.lang PersistentList IFn)))

(defn- make-cssfn-record
  "An internal CSSFunction function which takes the \"compiles-to\"
  function parameter, e.g. min\", a function which is applied to args
  during the compilation and the arguments and creates a CSSFunction record."
  [compiles-to* compile-fn* & args]
  (CSSFunction. compiles-to* compile-fn* args))

(defmacro defcssfn
  "Defines a CSS function. In most cases, you do NOT need to define a special compile-fn
  function - it should always be enough to use one of single-arg, spacejoin, commajoin.
  All of them compile the params, but: Single-arg gives you a warning if you give it more
  than 1 argument and compiles the args like commajoin. Commajoin compiles all its args
  and str/joins them with a comma. Spacejoin compiles all its args and str/joins them
  with a space. All these function also take the compiles-to argument and put it in front
  of a bracket enclosing the str/joined arguments.
  You can give this function 1, 2 or 3 arguments:

  (defcssfn translate)   (the default compile-fn is commajoin)
  (translate (u/px 80) (u/css-rem 6))   ... compiles to    \"translate(80px, 6rem)\"

  (defcssfn css-min \"min\")
  (css-min (u/px 500) (u/vw 40) (u/cm 20))   ... compiles to   \"min(500px, 40vw, 20cm)\"

  (defcssfn calc spacejoin)
  (calc (u/px 200) add 3 mul (u/percent 20))   ... compiles to   \"calc(200px + 3 * 20%)\"

  The arity(3) can be used like this:
  (defcssfn my-clj-fn \"css-fn\" spacejoin)
  (my-clj-fn (u/s 20) (u/ms 500))   ... compiles to   \"css-fn(20s 500ms)\""
  ([fn-name]
   (let [compiles-to (str fn-name)]
     `(defcssfn ~fn-name ~compiles-to nil)))
  ([fn-name css-fn-or-fn-tail]
   (condp instance? css-fn-or-fn-tail String `(defcssfn ~fn-name ~css-fn-or-fn-tail nil)
                                      PersistentList (let [compiles-to (str fn-name)]
                                                       `(defcssfn ~fn-name ~compiles-to ~css-fn-or-fn-tail))
                                      IFn (let [compiles-to (str fn-name)]
                                            `(defcssfn ~fn-name ~compiles-to ~css-fn-or-fn-tail))
                                      (throw (IllegalArgumentException.
                                               (str "Error defining a CSS function " fn-name " with arity(2):"
                                                    "\nThe second argument " css-fn-or-fn-tail " is"
                                                    " neither a string nor a function.")))))
  ([clojure-fn-name compiles-to compile-fn]
   `(def ~clojure-fn-name (partial ~make-cssfn-record ~compiles-to ~compile-fn))))

(defn commajoin
  "A CSSFunction util/str-commajoin compile function. Compiles the
  function to a form <fn-name>(arg1, arg2, arg3, ...),"
  [{:keys [compiles-to args]}]
  (str compiles-to "(" (->> args (map compile-expression)
                            util/str-commajoin) ")"))

(defn spacejoin
  "A CSSFunction util/str-spacejoin compile function. Compiles the
  function to a form <fn-name>(arg1 arg2 arg3 ...),"
  [{:keys [compiles-to args]}]
  (str compiles-to "(" (->> args (map compile-expression)
                            util/str-spacejoin) ")"))

(defn single-arg
  "A CSSFunction compile function. Presumes that only one arg is given.
  If not, calls commajoin function above and gives us a warning instead."
  [{:keys [compiles-to args] :as cssfn}]
  (if (= (count args) 1)
    (str compiles-to "(" (-> args first compile-expression) ")")
    (do (println (str "Warning: A CSSFunction \"" compiles-to "\" expects to have"
                      " 1 argument, instead got arguments: " args))
        (commajoin cssfn))))

(defn css-format-fn
  "A special function for css-format which also puts additional quotes around
  the parameter to enable usage of keywords."
  [{:keys [args] :as cssfn}]
  (if (= (count args) 1)
    (str "format(\"" (-> args first compile-expression) "\")")
    (do (println (str "Warning: A CSSFunction \"css-format\" expects to have"
                      " 1 argument, instead got arguments: " args))
        (commajoin cssfn))))

; https://www.quackit.com/css/functions/

;; single arg functions

(defcssfn blur single-arg)
(defcssfn brightness single-arg)
(defcssfn contrast single-arg)
(defcssfn css-format "format" css-format-fn)
(defcssfn grayscale single-arg)
(defcssfn hue-rotate single-arg)
(defcssfn invert single-arg)
(defcssfn perspective single-arg)
(defcssfn rotate single-arg)
(defcssfn rotateX single-arg)
(defcssfn rotateY single-arg)
(defcssfn rotateZ single-arg)
(defcssfn saturate single-arg)
(defcssfn sepia single-arg)
(defcssfn skewX single-arg)
(defcssfn skewY single-arg)
(defcssfn scaleX single-arg)
(defcssfn scaleY single-arg)
(defcssfn scaleZ single-arg)
(defcssfn translateX single-arg)
(defcssfn translateY single-arg)
(defcssfn translateZ single-arg)

;; comma-join functions

(defcssfn attr commajoin)
(defcssfn counter commajoin)
(defcssfn counters commajoin)
(defcssfn cubic-bezier commajoin)
(defcssfn css-filter "filter" commajoin)
(defcssfn hwb commajoin)
(defcssfn linear-gradient commajoin)
(defcssfn matrix commajoin)
(defcssfn matrix3d commajoin)
(defcssfn css-max "max" commajoin)
(defcssfn css-min "min" commajoin)
(defcssfn polygon commajoin)
(defcssfn radial-gradient commajoin)
(defcssfn repeating-linear-gradient commajoin)
(defcssfn repeating-radial-gradient commajoin)
(defcssfn rotate3d commajoin)
(defcssfn scale commajoin)
(defcssfn scale3d commajoin)
(defcssfn skew commajoin)
(defcssfn translate commajoin)
(defcssfn translate3d commajoin)
(defcssfn url commajoin)
(defcssfn css-var "var" commajoin)

;; space-join functions

(defcssfn calc spacejoin)
(defcssfn circle spacejoin)
(defcssfn drop-shadow spacejoin)
(defcssfn ellipse spacejoin)
(defcssfn image spacejoin)
(defcssfn inset spacejoin)
(defcssfn symbols spacejoin)