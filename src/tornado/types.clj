(ns tornado.types)

(defrecord CSSUnit [compiles-to value])

(defrecord CSSFunction [compiles-to args])

(defrecord CSSAtRule [identifier value])

(defrecord CSSColor [type value])

(defrecord CSSSelector [selector elements])

(defrecord CSSPseudoClass [pseudoclass parent])

(defrecord CSSPseudoElement [pseudoelement parent])