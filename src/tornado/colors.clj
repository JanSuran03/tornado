(ns tornado.colors
  (:require [tornado.types]
            [tornado.units]
            [tornado.util :as util]
            [clojure.string :as str])
  (:import (tornado.types CSSColor)
           (clojure.lang Keyword Symbol)))

(defn get-color-type
  "Returns "
  [color]
  (condp = (type color) CSSColor (:type color)
                        String String
                        Keyword Keyword
                        Symbol Symbol
                        (throw (IllegalArgumentException.
                                 (str "The given color is none of a tornado CSSColor record, color keyword,"
                                      " color symbol or a string: " color)))))

(def default-colors
  "Available default colors in tornado.
  Usage: {:color            :black
          :background-color :crimson}   etc.
  Tornado does the hex-code translation for you."
  ; See https://www.w3schools.com/cssref/css_colors.asp + ":font-black for recommended font color"
  {:aliceblue            "#F0F8FF"
   :antiquewhite         "#FAEBD7"
   :aqua                 "#00FFFF"
   :aquamarine           "#7FFFD4"
   :azure                "#F0FFFF"
   :beige                "#F5F5DC"
   :bisque               "#FFE4C4"
   :black                "#000000"
   :blanchedalmond       "#FFEBCD"
   :blue                 "#0000FF"
   :blueviolet           "#8A2BE2"
   :brown                "#A52A2A"
   :burlywood            "#DEB887"
   :cadetblue            "#5F9EA0"
   :chartreuse           "#7FFF00"
   :chocolate            "#D2691E"
   :coral                "#FF7F50"
   :cornflowerblue       "#6495ED"
   :cornsilk             "#FFF8DC"
   :crimson              "#DC143C"
   :cyan                 "#00FFFF"
   :darkblue             "#00008B"
   :darkcyan             "#008B8B"
   :darkgoldenrod        "#B8860B"
   :darkgray             "#A9A9A9"
   :darkgrey             "#A9A9A9"
   :darkgreen            "#006400"
   :darkkhaki            "#BDB76B"
   :darkmagenta          "#8B008B"
   :darkolivegreen       "#556B2F"
   :darkorange           "#FF8C00"
   :darkorchid           "#9932CC"
   :darkred              "#8B0000"
   :darksalmon           "#E9967A"
   :darkseagreen         "#8FBC8F"
   :darkslateblue        "#483D8B"
   :darkslategray        "#2F4F4F"
   :darkslategrey        "#2F4F4F"
   :darkturquoise        "#00CED1"
   :darkviolet           "#9400D3"
   :deeppink             "#FF1493"
   :deepskyblue          "#00BFFF"
   :dimgray              "#696969"                          ; nice
   :dimgrey              "#696969"                          ; nice
   :dodgerblue           "#1E90FF"
   :firebrick            "#B22222"
   :floralwhite          "#FFFAF0"
   :font-black           "#1A1B1F"                          ; recommended font color
   :forestgreen          "#228B22"
   :fuchsia              "#FF00FF"
   :gainsboro            "#DCDCDC"
   :ghostwhite           "#F8F8FF"
   :gold                 "#FFD700"
   :goldenrod            "#DAA520"
   :gray                 "#808080"
   :grey                 "#808080"
   :green                "#008000"
   :greenyellow          "#ADFF2F"
   :honeydew             "#F0FFF0"
   :hotpink              "#ADFF2F"
   :indianred            "#CD5C5C"
   :indigo               "#4B0082"
   :ivory                "#FFFFF0"
   :khaki                "#F0E68C"
   :lavender             "#E6E6FA"
   :lavenderblush        "#FFF0F5"
   :lawngreen            "#7CFC00"
   :lemonchiffon         "#FFFACD"
   :lightblue            "#ADD8E6"
   :lightcoral           "#F08080"
   :lightcyan            "#E0FFFF"
   :lightgoldenrodyellow "#FAFAD2"
   :lightgray            "#D3D3D3"
   :lightgrey            "#D3D3D3"
   :lightgreen           "#90EE90"
   :lightpink            "#FFB6C1"
   :lightsalmon          "#FFA07A"
   :lightseagreen        "#20B2AA"
   :lightskyblue         "#87CEFA"
   :lightslategray       "#778899"
   :lightslategrey       "#778899"
   :lightsteelblue       "#B0C4DE"
   :lightyellow          "#FFFFE0"
   :lime                 "#00FF00"
   :limegreen            "#32CD32"
   :linen                "#FAF0E6"
   :magenta              "#FF00FF"
   :maroon               "#800000"
   :mediumaquamarine     "#66CDAA"
   :mediumblue           "#0000CD"
   :mediumorchid         "#BA55D3"
   :mediumpurple         "#9370DB"
   :mediumseagreen       "#3CB371"
   :mediumslateblue      "#7B68EE"
   :mediumspringgreen    "#00FA9A"
   :mediumturquoise      "#48D1CC"
   :mediumvioletred      "#C71585"
   :midnightblue         "#191970"
   :mintcream            "#F5FFFA"
   :mistyrose            "#FFE4E1"
   :moccasin             "#FFE4B5"
   :navajowhite          "#FFDEAD"
   :navy                 "#000080"
   :oldlace              "#FDF5E6"
   :olive                "#808000"
   :olivedrab            "#6B8E23"
   :orange               "#FFA500"
   :orangered            "#FF4500"
   :orchid               "#DA70D6"
   :palegoldenrod        "#EEE8AA"
   :palegreen            "#98FB98"
   :paleturquoise        "#AFEEEE"
   :palevioletred        "#DB7093"
   :papayawhip           "#FFEFD5"
   :peachpuff            "#FFDAB9"
   :peru                 "#CD853F"
   :pink                 "#FFC0CB"
   :plum                 "#DDA0DD"
   :powderblue           "#B0E0E6"
   :purple               "#800080"
   :rebeccapurple        "#663399"
   :red                  "#FF0000"
   :rosybrown            "#BC8F8F"
   :royalblue            "#4169E1"
   :saddlebrown          "#8B4513"
   :salmon               "#FA8072"
   :sandybrown           "#F4A460"
   :seagreen             "#2E8B57"
   :seashell             "#FFF5EE"
   :sienna               "#A0522D"
   :silver               "#C0C0C0"
   :skyblue              "#87CEEB"
   :slateblue            "#6A5ACD"
   :slategray            "#708090"
   :slategrey            "#708090"
   :snow                 "#FFFAFA"
   :springgreen          "#00FF7F"
   :steelblue            "#4682B4"
   :tan                  "#D2B48C"
   :teal                 "#008080"
   :thistle              "#D8BFD8"
   :tomato               "#FF6347"
   :turquoise            "#40E0D0"
   :violet               "#EE82EE"
   :wheat                "#F5DEB3"
   :white                "#FFFFFF"
   :whitesmoke           "#F5F5F5"
   :yellow               "#FFFF00"
   :yellowgreen          "#9ACD32"})

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
     (CSSColor. "hsla" {:hue        hue
                        :saturation (util/percent->number saturation)
                        :lightness  (util/percent->number lightness)
                        :alpha      (util/percent->number alpha)})))
  ([hue saturation lightness]
   (hsla [hue saturation lightness 1]))
  ([hue saturation lightness alpha]
   (hsla [hue saturation lightness alpha])))

(defn rgb? [x]
  (and (instance? CSSColor x)
       (= (:type x) "rgb")))
(defn rgba? [x]
  (and (instance? CSSColor x)
       (= (:type x) "rgba")))
(defn hsl? [x]
  (and (instance? CSSColor x)
       (= (:type x) "hsl")))
(defn hsla? [x]
  (and (instance? CSSColor x)
       (= (:type x) "hsla")))

(defn hex? [x]
  (and (string? x)
       (let [[first & rest] x]
         (and (= first \#)
              (util/double-hex? (apply str rest))))))

(defn hex-no-alpha? [x]
  (and (hex? x)
       (= (count x) 7)))

(defn hex-alpha? [x]
  (and (hex? x)
       (= (count x) 9)))

(defn rgb->rgba [{:keys [value]}]
  (let [{:keys [red green blue]} value]
    (rgba [red green blue 1])))

(defn rgba->rgb
  "Used for converting rgba to rgb since #'hex->rgba always returns rgba.
  Sometimes we might want and rgb color from that."
  [{:keys [value]}]
  (let [{:keys [red green blue]} value]
    (rgb [red green blue])))

(defn hsl->hsla [{:keys [value]}]
  (let [{:keys [hue saturation lightness]} value]
    (hsla [hue saturation lightness 1])))

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

(defn rgb->hex
  "Converts an rgb/rgba CSSColor record to a hex-string. Rounds the hex-alpha of
  the color if the color is in rgba format."
  [{:keys [type value] :as color}]
  (if (instance? CSSColor color)
    (case type "rgb" (let [{:keys [red green blue]} value
                           [red green blue] (map #(Math/round %) [red green blue])
                           in-hex (map util/base10->double-hex-map [red green blue])]
                       (apply str "#" in-hex))
               "rgba" (let [{:keys [red green blue alpha]} value
                            [red green blue] (map #(Math/round %) [red green blue])
                            alpha (Math/round (float (* 255 alpha)))
                            in-hex (map util/base10->double-hex-map [red green blue alpha])]
                        (apply str "#" in-hex))
               :else (do (println (str "Unable to convert " color " to a hex-string -"
                                       " it is neither in rgb nor in rgba format."))
                         color))
    (throw (IllegalArgumentException. (str "Expected a CSSColor record: " color)))))

(defn hsl->rgb
  "https://www.rapidtables.com/convert/color/hsl-to-rgb.html
  Hsl to rgb. hsla to rgba.

  Unfortunately, Math/round abs Math/abs would throw an error
  with ratios, they need to be converted to floats first."
  [{:keys [value] :as hsl-color}]
  {:pre [(or (hsl? hsl-color) (hsla? hsl-color))]}
  (let [{:keys [hue saturation lightness alpha]} value
        C (* (- 1 (Math/abs (float (- (* 2 lightness) 1)))) saturation)
        X (* C (- 1 (Math/abs (float (- (mod (/ hue 60) 2) 1)))))
        m (- lightness (/ C 2))
        idx0 (mod (int (/ (+ 240 hue) 120)) 3)
        idxC (int (mod (+ idx0 1 (mod (/ hue 60) 2)) 3))
        idxX (int (mod (+ idx0 1 (mod (inc (/ hue 60)) 2)) 3))
        c-x-m [[0 idx0] [C idxC] [X idxX]]
        [[R' _] [G' _] [B' _]] (sort-by second < c-x-m)
        [R G B] (map #(Math/round (float (* (+ % m) 255))) [R' G' B'])]
    (if (hsl? hsl-color)
      (rgb R G B)
      (rgba R G B alpha))))

(defn rgb->hsl
  "https://www.rapidtables.com/convert/color/rgb-to-hsl.html
  Rgb to hsl, rgba to hsla."
  [{:keys [value] :as rgb-color}]
  {:pre [(or (rgb? rgb-color) (rgba? rgb-color))]}
  (let [{:keys [red green blue alpha]} value
        [R' G' B' :as rgb'] (map #(/ % 255) [red green blue])
        Cmax (apply max rgb')
        Cmin (apply min rgb')
        Crange (- Cmax Cmin)
        hue (cond (zero? Crange) 0
                  (= Cmax R') (* 60 (mod (/ (- G' B') Crange) 6))
                  (= Cmax G') (* 60 (+ (/ (- B' R') Crange) 2))
                  :else (* 60 (+ (/ (- R' G') Crange) 4)))
        lightness (util/avg Cmax Cmin)
        saturation (if (zero? Crange) 0 (/ Crange (- 1 (Math/abs (float (- (* 2 lightness) 1))))))
        [H S L] [(Math/round (float hue)) (util/round saturation) (util/round lightness)]]
    (if (rgb? rgb-color)
      (hsl H S L)
      (hsla H S L alpha))))

(defn- unknown-color-type [{:keys [type] :as color}]
  (throw (IllegalArgumentException. (str "Unknown color type: " type " of color " color))))

(defn ->rgb [{:keys [type] :as color}]
  (case type "rgb" color
             "rgba" (throw (IllegalArgumentException.
                             (str "Error: an rgba color is not convertible to rgb: " color)))
             "hsl" (hsl->rgb color)
             "hsla" (throw (IllegalArgumentException.
                             (str "Error: an hsla color is not convertible to rgb: " color)))
             (if (hex-no-alpha? color)
               (-> color hex->rgba rgba->rgb)
               (unknown-color-type color))))

(defn ->rgba [{:keys [type] :as color}]
  (case type "rgb" (rgb->rgba color)
             "rgba" color
             "hsl" (-> color hsl->rgb rgb->rgba)
             "hsla" (hsl->rgb color)
             (if (hex? color)
               (hex->rgba color)
               (unknown-color-type color))))
(defn ->hsl [{:keys [type] :as color}]
  (case type "rgb" (rgb->hsl color)
             "rgba" (throw (IllegalArgumentException.
                             (str "Error: an rgba color is not convertible to hsl: " color)))
             "hsl" color
             "hsla" (throw (IllegalArgumentException.
                             (str "Error: an hsla color is not convertible to hsl: " color)))
             (if (hex-no-alpha? color)
               (-> color hex->rgba rgba->rgb rgb->hsl)
               (unknown-color-type color))))
(defn ->hsla [{:keys [type] :as color}]
  (case type "rgb" (-> color rgb->hsl hsl->hsla)
             "rgba" (rgb->hsl color)
             "hsl" (hsl->hsla color)
             "hsla" color
             (if (hex? color)
               (-> color hex->rgba rgb->hsl)
               (unknown-color-type color))))

(defn has-alpha? [color]
  (or (rgb? color) (hsl? color) (hex-no-alpha? color)))

(defn with-alpha [color]
  (cond (or (rgb? color) (rgba? color)) (->rgba color)
        (or (hsl? color) (hsla? color)) (->hsla color)
        (hex? color) (hex->rgba color)
        :else (unknown-color-type color)))

(defn ->hsl?a [color]
  (if (has-alpha? color)
    (->hsl color)
    (->hsla color)))

(defn- range-0-1 [value]
  (util/in-range value 0 1))

(defn rotate-hue
  "Rotates hue of a color by a given angle in degrees for any color type,
  e.g. (rotate-hue \"#00ff00\" 90)"
  [color angle]
  (-> color ->hsl?a (update-in [:value :hue] #(mod (+ % angle) 360))))

(defn saturate
  "Saturates a color by a given value: 0.15, (percent 15), \"15%\" work the same way."
  [color value]
  (let [value (util/percent->number value)]
    (-> color ->hsl?a (update-in [:value :saturation] #(range-0-1 (+ % value))))))

(defn desaturate
  "Same as (saturate color value), but the value is subtracted instead."
  [color value]
  (saturate color (- (util/percent->number value))))

(defn lighten
  "Lightens a color by a given value: 0.15, (percent 15), \"15%\" work the same way."
  [color value]
  (let [value (util/percent->number value)]
    (-> color ->hsl?a (update-in [:value :lightness] #(range-0-1 (+ % value))))))

(defn darken
  "Same as (lighten color value), but the value is subtracted instead."
  [color value]
  (lighten color (- (util/percent->number value))))

(defn opacify
  "Opacifies a color by a given value: 0.15, (percent 15), \"15%\" work the same way."
  [color value]
  (-> color with-alpha (update-in [:value :alpha] #(range-0-1 (+ % value)))))

(defn transparentize
  "Same as (opacify color value), but the value is subtracted instead."
  [color value]
  (opacify color (- (util/percent->number value))))

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
    (CSSColor. "rgb" {:red   (util/apply-avg red-vals)
                      :green (util/apply-avg green-vals)
                      :blue  (util/apply-avg blue-vals)})))

(defmethod -mix-colors "rgba"
  [_ colors]
  (let [values (map :value colors)
        red-vals (map :red values)
        green-vals (map :green values)
        blue-vals (map :blue values)
        alpha-vals (map :alpha values)]
    (CSSColor. "rgba" {:red   (util/apply-avg red-vals)
                       :green (util/apply-avg green-vals)
                       :blue  (util/apply-avg blue-vals)
                       :alpha (util/apply-avg alpha-vals)})))

(defmethod -mix-colors "hsl"
  [_ colors]
  (let [values (map :value colors)
        hue-vals (map :hue values)
        saturation-vals (map :saturation values)
        lightness-vals (map :lightness values)]
    (CSSColor. "hsl" {:hue        (util/apply-avg hue-vals)
                      :saturation (util/apply-avg saturation-vals)
                      :lightness  (util/apply-avg lightness-vals)})))

(defmethod -mix-colors "hsla"
  [_ colors]
  (let [values (map :value colors)
        hue-vals (map :hue values)
        saturation-vals (map :saturation values)
        lightness-vals (map :lightness values)
        alpha-vals (map :alpha values)]
    (CSSColor. "hsla" {:hue        (util/apply-avg hue-vals)
                       :saturation (util/apply-avg saturation-vals)
                       :lightness  (util/apply-avg lightness-vals)
                       :alpha      (util/apply-avg alpha-vals)})))

(defn mix-colors
  ([color]
   color)
  ([color1 & more]
   (let [colors (list* color1 more)
         types (->> colors (map get-color-type) (filter string?) (#(if (seq %) % ["rgba"])))
         colors (map (fn [color]
                       (get default-colors color color)) colors)
         some-alpha-hex? (some hex-alpha? colors)
         not-symbols? (not-any? symbol? colors)]
     (if (and not-symbols?)
       (let [dominant-type (->> types frequencies (sort-by second >)
                                (sort-by (fn [[color-type _]]
                                           (if (= (last color-type) \a)
                                             1 0)) >)
                                ffirst)
             dominant-type (if (and some-alpha-hex? (not (str/ends-with? dominant-type "a")))
                             (str dominant-type "a")
                             dominant-type)
             conversion-fn (case dominant-type "rgb" #'->rgb
                                               "rgba" #'->rgba
                                               "hsl" #'->hsl
                                               "hsla" #'->hsla)
             converted-colors (map conversion-fn colors)]
         (-mix-colors dominant-type converted-colors))
       (throw (IllegalArgumentException. (str "Can't mix colors of different types: " colors)))))))