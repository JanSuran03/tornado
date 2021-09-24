(ns tornado.functions
  "Everything related to CSS functions."
  (:require [tornado.types]
            [tornado.util :as util]
            [tornado.compiler :refer [compile-expression]])
  #?(:cljs (:require-macros [tornado.functions :refer [defcssfn]]))
  #?(:clj (:import (tornado.types CSSFunction)
                   (clojure.lang PersistentList IFn))))

#?(:clj
   (defn- make-cssfn-record
     "An internal CSSFunction function which takes the \"compiles-to\"
     function parameter, e.g. min\", a function which is applied to args
     during the compilation and the arguments and creates a CSSFunction record."
     [compiles-to* compile-fn* & args]
     (CSSFunction. compiles-to* compile-fn* args)))

#?(:clj
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

     The arity(3) can be used like this to combine both previous features of the arity(2):
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
      `(def ~clojure-fn-name (partial ~make-cssfn-record ~compiles-to ~compile-fn)))))

(defn comma-join
  "A CSSFunction util/str-commajoin compile function. Compiles the
  function to a form <fn-name>(arg1, arg2, arg3, ...),"
  [{:keys [compiles-to args]}]
  (str compiles-to "(" (->> args (map identity #_compile-expression)
                            util/str-commajoin) ")"))

(defn space-join
  "A CSSFunction util/str-spacejoin compile function. Compiles the
  function to a form <fn-name>(arg1 arg2 arg3 ...),"
  [{:keys [compiles-to args]}]
  (str compiles-to "(" (->> args (map identity #_compile-expression)
                            util/str-spacejoin) ")"))

(defn single-arg
  "A CSSFunction compile function. Presumes that only one arg is given.
  If not, calls commajoin function above and gives us a warning instead."
  [{:keys [compiles-to args] :as cssfn}]
  (if (= (count args) 1)
    (str compiles-to "(" (-> args first identity #_compile-expression) ")")
    (do (println (str "Warning: A CSSFunction \"" compiles-to "\" expects to have"
                      " 1 argument, instead got arguments: " args))
        (comma-join cssfn))))

(defn css-format-fn
  "A special function for css-format which also puts additional quotes around
  the parameter to enable usage of keywords."
  [{:keys [args] :as cssfn}]
  (if (= (count args) 1)
    (str "format(\"" (-> args first identity #_compile-expression) "\")")
    (do (println (str "Warning: A CSSFunction \"css-format\" expects to have"
                      " 1 argument, instead got arguments: " args))
        (comma-join cssfn))))

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

(defcssfn attr comma-join)
(defcssfn counter comma-join)
(defcssfn counters comma-join)
(defcssfn cubic-bezier comma-join)
(defcssfn css-filter "filter" comma-join)
(defcssfn hwb comma-join)
(defcssfn linear-gradient comma-join)
(defcssfn matrix comma-join)
(defcssfn matrix3d comma-join)
(defcssfn css-max "max" comma-join)
(defcssfn css-min "min" comma-join)
(defcssfn polygon comma-join)
(defcssfn radial-gradient comma-join)
(defcssfn repeating-linear-gradient comma-join)
(defcssfn repeating-radial-gradient comma-join)
(defcssfn rotate3d comma-join)
(defcssfn scale comma-join)
(defcssfn scale3d comma-join)
(defcssfn skew comma-join)
(defcssfn translate comma-join)
(defcssfn translate3d comma-join)
(defcssfn url comma-join)
(defcssfn css-var "var" comma-join)

;; space-join functions

(defcssfn calc space-join)
(defcssfn circle space-join)
(defcssfn drop-shadow space-join)
(defcssfn ellipse space-join)
(defcssfn image space-join)
(defcssfn inset space-join)
(defcssfn symbols space-join)