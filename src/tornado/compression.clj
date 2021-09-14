(ns tornado.compression
  "A namespace for compression of the compiled CSS file."
  (:require [clojure.string :as str]))

;; Yes, I am lazy, and I did not care about this namespace at all. Right now, it is not
;; my priority to write better RegExps. Maybe I will improve this namespace later.

(defn ^String compress-newlines
  "Transforms newlines to spaces."
  [^String expr]
  (str/replace expr #"\n+" " "))

(defn ^String reduce-spaces
  "Reduces consecutive spaces to one space."
  [^String expr]
  (str/replace expr #"\ +" " "))

(defn ^String remove-spaces-after-commas
  "Removes spaces directly after commas."
  [^String expr]
  (str/replace expr #"\, " ","))

(defn ^String remove-spaces-after-semicolons
  "Removes spaces directly after semicolons."
  [^String expr]
  (str/replace expr #"\; " ";"))

(defn ^String remove-spaces-after-colons
  "Removes spaces directly after colons."
  [^String expr]
  (str/replace expr #"\: " ":"))

(defn ^String remove-spaces-around-brackets
  "Removes spaces before and after all types of brackets."
  [^String expr]
  (-> expr (str/replace #"\{ " "{")
      (str/replace #" \{" "{")
      (str/replace #"\} " "}")
      (str/replace #" \}" "}")
      (str/replace #"\[ " "[")
      (str/replace #" \[" "[")
      (str/replace #"\] " "]")
      (str/replace #" \]" "]")
      (str/replace #"\( " "(")
      (str/replace #" \(" "(")
      (str/replace #"\) " ")")
      (str/replace #" \)" ")")))

(defn ^String compress
  "Compresses a stylesheet."
  [^String expr]
  (-> expr compress-newlines
      reduce-spaces
      remove-spaces-after-commas
      remove-spaces-after-semicolons
      remove-spaces-after-colons
      remove-spaces-around-brackets))