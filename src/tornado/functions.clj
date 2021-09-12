(ns tornado.functions
  (:require [tornado.types]
            [tornado.compiler :refer [compile-expression]]
            [tornado.util :as util])
  (:import (tornado.types CSSFunction)
           (clojure.lang PersistentList IFn)))

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

; https://www.quackit.com/css/functions/

(defcssfn attr commajoin)
(defcssfn blur single-arg)
(defcssfn brightness single-arg)
(defcssfn calc spacejoin)
(defcssfn circle spacejoin)
(defcssfn contrast single-arg)
(defcssfn counter commajoin)
(defcssfn counters commajoin)
(defcssfn cubic-bezier commajoin)
(defcssfn drop-shadow spacejoin)
(defcssfn ellipse spacejoin)
(defcssfn css-filter "filter" commajoin)
(defcssfn grayscale single-arg)
(defcssfn hue-rotate single-arg)
(defcssfn hwb commajoin)
(defcssfn image spacejoin)
(defcssfn inset spacejoin)
(defcssfn invert single-arg)
(defcssfn linear-gradient commajoin)
(defcssfn matrix commajoin)
(defcssfn matrix3d commajoin)
(defcssfn css-max "max" commajoin)
(defcssfn css-min "min" commajoin)
(defcssfn opacity single-arg)
(defcssfn perspective single-arg)
(defcssfn polygon commajoin)
(defcssfn radial-gradient commajoin)
(defcssfn repeating-linear-gradient commajoin)
(defcssfn repeating-radial-gradient commajoin)
(defcssfn rotate single-arg)
(defcssfn rotate3d commajoin)
(defcssfn rotateX single-arg)
(defcssfn rotateY single-arg)
(defcssfn rotateZ single-arg)
(defcssfn saturate single-arg)
(defcssfn sepia single-arg)
(defcssfn scale commajoin)
(defcssfn scale3d commajoin)
(defcssfn scaleX single-arg)
(defcssfn scaleY single-arg)
(defcssfn scaleZ single-arg)
(defcssfn skew commajoin)
(defcssfn skewX single-arg)
(defcssfn skewY single-arg)
(defcssfn symbols spacejoin)
(defcssfn translate commajoin)
(defcssfn translate3d commajoin)
(defcssfn translateX single-arg)
(defcssfn translateY single-arg)
(defcssfn translateZ single-arg)
(defcssfn url commajoin)
(defcssfn css-var "var" commajoin)

;; symbols for calc function
(def add "+")
(def sub "-")
(def mul "*")
(def div "/")