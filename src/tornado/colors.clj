(ns tornado.colors
  (:require [tornado.types]
            [tornado.units]
            [tornado.util :as util])
  (:import (tornado.types CSSColor)))

(defn get-color-type
  ""
  [color]
  (condp = (type color) CSSColor (:type color)
                        String String
                        (throw (IllegalArgumentException. (str "The given color is neither a tornado"
                                                               " CSSColor record nor a string.")))))

(def tornado-colors
  "Available default colors in tornado."
  {:black  "#000000"
   :blue   "#0000FF"
   :gray   "#808080"
   :green  "#00FF00"
   :red    "#FF0000"
   :white  "#FFFFFF"
   :yellow "#FFFF00"})

(defn rgb
  "Creates an rgb CSSColor record."
  ([[red green blue]]
   (if (every? #(util/between % 0 255) [red green blue])
     (CSSColor. "rgb" {:red red, :green green, :blue blue})
     (throw (IllegalArgumentException. (str "All values of an rgb color must be between 0 and 255: "
                                            red ", " green ", " blue)))))
  ([red green blue]
   (rgb [red green blue])))

(defn rgba
  "Creates an rgba CSSColor record."
  ([[red green blue alpha]]
   (let [alpha (or alpha 1)]
     (if (and (every? #(util/between % 0 255) [red green blue]) (util/between alpha 0 1))
       (CSSColor. "rgba" {:red red, :green green, :blue blue, :alpha (util/percent->number alpha true)})
       (throw (IllegalArgumentException.
                (str "All r, g, b values of an rgb color must be between 0 and 255: "
                     red ", " green ", " blue " and alpha between 0 and 1: " alpha))))))
  ([red green blue]
   (rgba [red green blue 1]))
  ([red green blue alpha]
   (rgba [red green blue alpha])))

(defn hsl
  "Creates an hsl CSSColor record."
  ([[hue saturation lightness]]
   (CSSColor. "hsl" {:hue        hue
                     :saturation (util/percent->number saturation)
                     :lightness  (util/percent->number lightness)}))
  ([hue saturation lightness]
   (hsl [hue saturation lightness])))

(defn hsla
  "Creates an hsla CSSColor record."
  ([[hue saturation lightness alpha]]
   (let [alpha (or alpha 1)]
     (CSSColor. "hsl" {:hue        hue
                       :saturation (util/percent->number saturation)
                       :lightness  (util/percent->number lightness)
                       :alpha      (util/percent->number alpha)})))
  ([hue saturation lightness]
   (hsla [hue saturation lightness 1]))
  ([hue saturation lightness alpha]
   (hsla [hue saturation lightness alpha])))

(defn hex->rgba
  "Converts a color in hexadecimal string to an rgba color:

  (hex->rgba \"#20FF3f\")
  => #tornado.types.CSSColor{:type \"rgba\", :value {:red   32
                                                   :green 255,
                                                   :blue  63
                                                   :alpha 1}}

  (hex->rgba \"#20FF3f\" 0.3)
  => #tornado.types.CSSColor{:type \"rgba\", :value {:red   32
                                                   :green 255,
                                                   :blue  63
                                                   :alpha 0.3}}

  (hex->rgba \"#20FF3f3f\")
  => #tornado.types.CSSColor{:type \"rgba\", :value {:red   32
                                                   :green 255
                                                   :blue  63
                                                   :alpha 0.24609375}}

  (hex->rgba \"#20FF3f\" \"20 %\")
  => #tornado.types.CSSColor{:type \"rgba\", :value {:red   32
                                                   :green 255
                                                   :blue  63
                                                   :alpha 0.2}}

  As you can see, this conversion is not case-sensitive.
  Do not use tornado.units/percent as the second argument!"
  ([color]
   (hex->rgba color 1))
  ([color alpha]
   (let [alpha (some-> alpha util/percent->number)]
     (as-> color <> (subs <> 1) (partition 2 <>)
           (map #(apply str %) <>)
           (mapv #(get util/double-hex->base10-map %) <>)
           (if (= (count <>) 3)
             (conj <> alpha)
             (update <> 3 #(util/int* (/ % 256))))
           (rgba <>)))))

(defn rgb?a->hex
  "Converts an rgb/rgba CSSColor record to a hex-string. Rounds the hex-alpha of
  the color if the color is in rgba format."
  [{:keys [type value] :as color}]
  (if (instance? CSSColor color)
    (case type "rgb" (let [{:keys [red green blue]} value
                           in-hex (map util/base10->double-hex-map [red green blue])]
                       (apply str "#" in-hex))
               "rgba" (let [{:keys [red green blue alpha]} value
                            alpha (Math/round (* 256 alpha))
                            in-hex (map util/base10->double-hex-map [red green blue alpha])]
                        (apply str "#" in-hex))
               :else (do (println (str "Unable to convert " color " to a hex-string -"
                                       " it is neither in rgb nor in rgba format."))
                         color))
    (throw (IllegalArgumentException. (str "Expected a CSSColor record: " color)))))

(defmulti -mix-colors
          "Calls a relevant function to compute the average of more colors."
          (fn [color-type _]
            color-type))

(defmethod -mix-colors "rgb"
  [_ colors]
  (let [values (map :value colors)
        red-vals (map :red values)
        green-vals (map :green values)
        blue-vals (map :blue values)]
    (CSSColor. "rgb" {:red   (apply util/avg red-vals)
                      :green (apply util/avg green-vals)
                      :blue  (apply util/avg blue-vals)})))

(defmethod -mix-colors "rgba"
  [_ colors]
  (let [values (map :value colors)
        red-vals (map :red values)
        green-vals (map :green values)
        blue-vals (map :blue values)
        alpha-vals (map :alpha values)]
    (CSSColor. "rgba" {:red   (apply util/avg red-vals)
                       :green (apply util/avg green-vals)
                       :blue  (apply util/avg blue-vals)
                       :alpha (apply util/avg alpha-vals)})))

(defmethod -mix-colors "hsl"
  [_ colors]
  (let [values (map :value colors)
        hue-vals (map :hue values)
        saturation-vals (map :saturation values)
        lightness-vals (map :lightness values)]
    (CSSColor. "hsl" {:hue        (apply util/avg hue-vals)
                      :saturation (apply util/avg saturation-vals)
                      :lightness  (apply util/avg lightness-vals)})))

(defmethod -mix-colors "hsla"
  [_ colors]
  (let [values (map :value colors)
        hue-vals (map :hue values)
        saturation-vals (map :saturation values)
        lightness-vals (map :lightness values)
        alpha-vals (map :alpha values)]
    (CSSColor. "hsla" {:hue        (apply util/avg hue-vals)
                       :saturation (apply util/avg saturation-vals)
                       :lightness  (apply util/avg lightness-vals)
                       :alpha      (apply util/avg alpha-vals)})))

(defn mix-colors
  ([color]
   color)
  ([color1 & more]
   (let [colors (into [color1] more)
         types (map get-color-type colors)
         not-strings? (every? #(not (string? %)) colors)]
     (if (and (apply = types) not-strings?)
       (-mix-colors (first types) colors)
       (throw (IllegalArgumentException. (str "Can't mix colors of different types: " colors)))))))