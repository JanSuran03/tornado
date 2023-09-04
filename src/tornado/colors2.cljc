(ns tornado.colors2)

(defprotocol ICSSColor)

(defprotocol ICSSAlpha)

(defprotocol IHex)

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
