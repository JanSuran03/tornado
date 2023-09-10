(ns tornado.colors.conversion
  (:require [tornado.colors.util :as color-util]
            [tornado.util :as util]))

(defmulti convert-impl
          "Converts a color to the given type."
          (fn [_color color-type desired-type]
            [color-type desired-type]))

(defn- invalid-conversion [color color-type desired-type & append]
  (util/exception (apply str "Cannot perform conversion of " color
                         (when color-type (str " (type=" color-type ")"))
                         " to " desired-type ": " append)))

(defn default-conversion [color color-type desired-type]
  (cond (= (str color-type) (str desired-type "a"))         ; lose alpha channel
        (if (map? color)
          (-> color (dissoc :alpha) (assoc :type desired-type))
          (util/exception "TODO alpha->"))

        (= (str color-type "a") desired-type)               ; gain alpha channel
        (if (map? color)
          (-> color (assoc :alpha 1) (assoc :type desired-type))
          (util/exception "TODO ->alpha"))

        :else
        (invalid-conversion color color-type desired-type "conversion not defined.")))

(let [invalid-conversion (fn [color color-type desired-type & append]
                           (util/exception (apply str "Cannot perform conversion of " color " (type="
                                                  color-type ") to " desired-type ": " append)))]
  (defmethod convert-impl :default
    [color color-type desired-type]
    (cond (nil? color-type)
          (invalid-conversion color color-type desired-type "not a color.")

          (identical? color-type desired-type)
          color

          (nil? (color-util/color-types desired-type))
          (invalid-conversion color color-type desired-type "not a valid desired color type.")

          :else
          (default-conversion color color-type desired-type))))

(defn convert [color desired-type]
  (convert-impl color (color-util/type-of-color color) desired-type))
