(ns tornado.functions
  (:require [tornado.types]
            [tornado.util :refer [general-parser-fn]])
  (:import (tornado.types CSSFunction)
           (clojure.lang PersistentList)))

#_(defn make-css-fn
    "Creates a CSS function which accepts any number of arguments and allows us to"
    [function num-args]
    (fn [& args]
      (CSSFunction. function num-args args)))

#_(defmacro defcssfn
    "Creates a CSS function, where then a function (`identifier` <args>) can be used in
    code to pass the function arguments. The number of args is used for compiler warnings,
    where wrong number of args in num-args vector will give us a warning. An empty or nil
    num-args vector will not give us any warning (e.g. min(& args)). The compiler then
    expands the fn and args to fn(arg1, arg2, ...),

    Arity (1):
       (defcssfn some-fn)
       => #'tornado.functions/some-fn
       (some-fn \"arg1\" \"arg2\")
       => #tornado.types.CSSFunction{:compiles-to \"some-fn\"
                                     :num-args    nil
                                     :args        (\"arg1\" \"arg2\")
    - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    Arity (2):
       (defcssfn min* \"min\")
       => #'tornado.functions/min*
       (min* \"30px\" \"2vw\" \"2rem\")
       => #tornado.types.CSSFunction{:compiles-to \"min\"
                                     :num-args    nil
                                     :args        (\"30px\" \"2vw\" \"2rem\")

       (defcssfn translate3d 3)  ; also supports a vector of args, if that fn is variadic
       => #'tornado.functions.translate3d
       (translate3d \"40px\" \"50px\" \"60px\")
       => #tornado.types.CSSFunction{:compiles-to \"translate3d\"
                                     :num-args    [3]
                                     :args        (\"40px\" \"50px\" \"60px\")
    - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    Arity (3):
       (defcssfn max-of-two 2 \"max\")
       => #'tornado.functions.max-of-two
       (max-of-two \"50vw\" \"500px\")
       => #tornado.types.CSSFunction{:compiles-to \"max\"
                                     :num-args    [2]
                                     :args        (\"50vw\" \"500px\")

    Arity (4):
       (defcssfn min-of-two 2 \"min\" \"Takes the minimum of 2 arguments.\")
       ... etc., see arity(3)."
    ([fn-name]
     (let [compiles-to (str fn-name)]
       `(defcssfn ~fn-name nil ~compiles-to nil)))
    ([fn-name args-or-css-fn]
     (cond (vector? args-or-css-fn) (let [compiles-to (str fn-name)]
                                      `(defcssfn ~fn-name ~args-or-css-fn ~compiles-to nil))
           (number? args-or-css-fn) (let [compiles-to (str fn-name)
                                          args [args-or-css-fn]]
                                      `(defcssfn ~fn-name ~args ~compiles-to nil))
           (string? args-or-css-fn) `(defcssfn ~fn-name nil ~args-or-css-fn nil)
           :else (throw (IllegalArgumentException.
                          (str "Error while defining a CSS function with arity (2): The second"
                               " argument is none of a vector, a number or a string: " args-or-css-fn)))))
    ([fn-name num-args compiles-to]
     `(defcssfn ~fn-name ~num-args ~compiles-to nil))
    ([fn-name num-args compiles-to doc]
     `(do (def ~fn-name (make-css-fn ~compiles-to ~num-args))
          (alter-meta! #'~fn-name assoc :doc ~doc))))

#_(defn make-css-fn [function]
    (fn [& args]
      (CSSFunction. function)))

(defmacro make-css-fn [fn-name]
  `(defn fn-name))

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
  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
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
     `(defcssfn ~fn-name ~compiles-to ~#'general-parser-fn)))
  ([fn-name css-fn-or-fn-tail]
   (condp instance? css-fn-or-fn-tail String `(defcssfn ~fn-name ~css-fn-or-fn-tail ~#'general-parser-fn)
                                      PersistentList (let [compiles-to (str fn-name)]
                                                       `(defcssfn ~fn-name ~compiles-to ~css-fn-or-fn-tail))
                                      (throw (IllegalArgumentException.
                                               (str "Error defining a CSS function fn-name with arity(2):"
                                                    "\nThe second argument " css-fn-or-fn-tail " is"
                                                    " neither a string nor a function.")))))
  ([fn-name compiles-to compile-fn]
   (let [cssfn-function (fn [compiles-to* compile-fn* & args]
                          (CSSFunction. compiles-to* compile-fn* args))]
     `(def ~fn-name (partial ~cssfn-function ~compiles-to ~compile-fn)))))

(defcssfn scale)

;(defn )

(comment (defcssfn translate [1 2])
         (defcssfn translate3d 3)
         (defcssfn translateX 1)
         (defcssfn translateY 1)
         (defcssfn translateZ 1)
         (defcssfn scale 1)
         (defcssfn min* "min")
         (defcssfn max* "max")
         (defcssfn rotate 1)
         (defcssfn rotateX 1)
         (defcssfn rotateY 1)
         (defcssfn rotateZ 1)
         (defcssfn rotate3d 3)
         (defcssfn cubic-bezier 4))