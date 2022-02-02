(ns tornado.test.colors
  (:require [clojure.test :refer :all]
            [tornado.core :refer :all]
            [tornado.colors :as colors]
            [clojure.string :as str]))

(deftest equal-red?
  (is (true? (->> [:red "red" 'red (colors/rgb->hex (rgb 255 0 0))
                   (-> :green (with-hue 0) (with-lightness 0.5) colors/hsl->rgb colors/rgb->hex)]
                  (map (comp str/lower-case compile-expression))
                  (apply =)))))

(deftest test*
  (is (= (-> :chocolate (with-hue 273) (with-saturation 0.56)
             (with-alpha 0.8) triad-next (scale-lightness 0.7)
             colors/->rgba compile-expression)
         "rgba(131, 89, 37, 0.8)")))