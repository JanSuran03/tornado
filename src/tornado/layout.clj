(ns tornado.layout
  (:require [clojure.string :as str]
            [tornado.util :as util]))

(defn grid-areas
  ""
  [[first-row & more :as all-rows]]
  (assert (and (every? sequential? all-rows)
               (every? #(every? util/valid? %) all-rows))
          "All grid areas must be sequences of symbols, keywords or strings.")
  (let [length (count first-row)]
    (if (every? #(= length (count %)) more)
      (->> all-rows (map (fn [row]
                           (let [row-str (str/join " " (map name row))]
                             (str "\"" row-str "\"")))))
      (throw (IllegalArgumentException.
               (str "Vectors in all grid rows must have the same length: " all-rows))))))