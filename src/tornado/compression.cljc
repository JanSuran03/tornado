(ns tornado.compression
  "A namespace for compression of the compiled CSS file."
  (:require
   [clojure.string :as str]))


(defn compress-whitespace
  ^String [expr]
  (str/replace expr #"\s+" " "))

(defn strip-whitespace-around
  "Remove whitespace before and after these characters: `,;:{}[]()`."
  ^String [expr]
  (str/replace expr #"\s*([,;:{}\[\]\(\)])\s*" "$1"))

(defn compress
  "Compress a stylesheet by removing any extra whitespace."
  ^String [^String expr]
  (-> expr
      compress-whitespace
      strip-whitespace-around))
