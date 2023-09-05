(ns tornado.colors2
  (:require [clojure.string :as str]
            [tornado.context :as ctx]
            [tornado.util :as util]))

(def default-colors
  "Available default colors in tornado.
  Usage: {:color            :black
          :background-color :crimson
          :border           [[(u/px 1) :solid :yellow-green]]}   etc.
  Tornado does the hex-code translation for you.
  Also, you can use both e.g. :yellow-green and :yellowgreen (or in string and
  symbol-forms, respectively), it all compiles to the same color."
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
   :fontblack            "#1A1B1F"                          ; recommended font color
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

(defprotocol ICSSColor
  (to-css [this] "Compiles the color to CSS.")
  (->hex [this])
  (->hex-alpha [this]))

(defprotocol ICSSAlpha)

(defprotocol IHex)

(defprotocol IRgbConvertible
  (->rgb [this] "Converts a color to rgb")
  (->rgba [this] "Converts a color to rgba"))

(defprotocol IHslConvertible
  (->hsl [this] "Converts a color to hsl")
  (->hsla [this] "Converts a color to hsla"))

(defprotocol IWithAlpha
  (with-alpha [this] "Changes a color's alpha, keeps all color channels."))

(defprotocol IWithRgb
  (with-red [this] "Changes a color's red channel, keeps green, blue and alpha.")
  (with-green [this] "Changes a color's green channel, keeps red, blue and alpha.")
  (with-blue [this] "Changes a color's blue channel, keeps red, green and alpha."))

(defprotocol IWithHsl
  (with-hue [this] "Changes a color's hue, keeps saturation, lightness and alpha.")
  (with-saturation [this] "Changes a color's saturation, keeps hue, lightness and alpha")
  (with-lightness [this] "Changes a color's lightness, keeps hue, saturation and alpha"))

(defrecord Rgb [red green blue]
  ICSSColor)

(defrecord Rgba [red green blue alpha]
  ICSSColor
  ICSSAlpha)

(defrecord Hsl [hue saturation lightness]
  ICSSColor)

(defrecord Hsla [hue saturation lightness alpha]
  ICSSColor
  ICSSAlpha)

(defn hex? [s]
  (and (string? s)
       (and (= (util/char-at s 0) \#)
            (util/double-hex? (subs s 1)))))

(defn non-alpha-hex? [x]
  (and (hex? x)
       (= (count x) 7)))

(defn alpha-hex? [x]
  (and (hex? x)
       (= (count x) 9)))

(defn color->1-word
  "Given a named object (string/symbol/keyword) removes dashes and returns it as a keyword."
  [color]
  (-> color name
      (str/replace #"\-" "")
      keyword))

(defn color-literal?
  "Returns true iff the given color is a string/symbol/keyword and is contained in the
  default colors map. Extra dashes are allowed."
  [x]
  (and (util/named? x)
       (contains? default-colors (color->1-word x))))

(defn rgb
  "Creates an Rgb color record."
  ([-rgb]
   (let [[red green blue] (cond (map? -rgb) ((juxt :red :green :blue) -rgb)
                                (vector? -rgb) -rgb
                                :else (util/exception (str "Cannot build RGB color from: " (util/or-nil -rgb))))]
     (if (every? #(and (int? %)
                       (util/between? % 0 255))
                 [red green blue])
       (Rgb. red green blue)
       (util/exception (str "All values of an rgb color must be between 0 and 255: "
                            red ", " green ", " blue)))))
  ([red green blue] (rgb [red green blue])))

(defn rgba
  "Creates an Rgba color record."
  ([-rgba]
   (let [[red green blue alpha] (cond (map? -rgba) ((juxt :red :green :blue :alpha) -rgba)
                                      (vector? -rgba) -rgba
                                      :else (util/exception (str "Cannot build RGBA color from: " (util/or-nil -rgba))))
         alpha (or alpha 1)]
     (if (and (util/between? alpha 0 1)
              (every? #(and (int? %)
                            (util/between? % 0 255))
                      [red green blue]))
       (Rgba. red green blue alpha)
       (util/exception (str "All values of an rgba color must be between 0 and 255: "
                            red ", " green ", " blue " and alpha between 0 and 1: " alpha)))))
  ([red green blue alpha] (rgba [red green blue alpha])))

(defn hsl
  "Creates an Hsl color record."
  ([-hsl]
   (let [[hue saturation lightness] (cond (map? -hsl) ((juxt :hue :saturation :lightness) -hsl)
                                          (vector? -hsl) -hsl
                                          :else (util/exception (str "Cannot build HSL color from: " (util/or-nil -hsl))))]
     (Hsl. hue (util/percent->number saturation) (util/percent->number lightness))))
  ([hue saturation lightness] (hsl [hue saturation lightness])))

(defn hsla
  "Creates an Hsla color record."
  ([-hsl]
   (let [[hue saturation lightness alpha] (cond (map? -hsl) ((juxt :hue :saturation :lightness) -hsl)
                                                (vector? -hsl) -hsl
                                                :else (util/exception (str "Cannot build HSL color from: " (util/or-nil -hsl))))
         alpha (or alpha 1)]
     (Hsla. hue (util/percent->number saturation) (util/percent->number lightness) (util/percent->number alpha))))
  ([hue saturation lightness] (hsla [hue saturation lightness])))

(defn rgb? [x]
  (instance? Rgb x))

(defn rgba? [x]
  (instance? Rgba x))

(defn hsl? [x]
  (instance? Hsl x))

(defn hsla? [x]
  (instance? Hsla x))

(defn color? [x]
  (or (satisfies? ICSSColor x)
      (color-literal? x)
      (hex? x)))

;; ------------------------------------- TEST -------------------------------------

(defmacro with-err-out [& body]
  `(binding [*out* *err*]
     ~@body))

(defmacro test-one
  "All the color tests be moved to the test namespaces after being ready to replace
  the older API, now kept here for simplicity and ease of development"
  [test-name & body]
  (let [[gres gfail] (repeatedly gensym)]
    `(let [~gres (try ~@body
                      (catch Throwable t#
                        t#))
           ~gfail ~(str "Failed on test case: " test-name ", form: " body)]
       (if (or (not ~gres) (instance? Throwable ~gres))
         (with-err-out (println (str ~gfail
                                     (when ~gres
                                       (str ", reason: " (.getMessage ~gres))))))))))

(defmacro test-multiple
  [test-name & forms]
  `(do ~@(map-indexed (fn [i form]
                        `(test-one ~(keyword (str (name test-name) "-" (inc i)))
                                   ~form))
                      forms)))

(test-multiple :test-color-instance
  (color? (rgb 1 2 3))
  (color? (rgba 1 2 3 0.1))
  (color? (hsl 1 1 1))
  (color? (hsla 1 1 1))
  (color? :darkblue)
  (color? :dark-blue)
  (color? 'darkblue)
  (color? 'dark-blue)
  (color? "darkblue")
  (color? "dark-blue")
  (not (color? :darqblue))
  (color? "#123456")
  (color? "#12345678")
  (not (color? "123456"))
  (not (color? "1234567"))
  (not (color? "#12345z"))
  (not (color? "#1234567z")))


