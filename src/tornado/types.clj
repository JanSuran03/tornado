(ns tornado.types
  "Internal CSS records used by tornado.")

(defrecord CSSUnit [compiles-to value])

(defrecord CSSFunction [compiles-to compile-fn args])

(defrecord CSSAtRule [identifier value])

(defrecord CSSColor [type value])

(defrecord CSSSelector [selector elements])

(defrecord CSSPseudoClass [pseudoclass parent])

(defrecord CSSPseudoElement [pseudoelement parent])