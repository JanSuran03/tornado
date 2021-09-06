(ns tornado.test-hiccup
  (:require [clojure.test :refer :all]
            [tornado.at-rules :as stylesheet]
            [tornado.units :as u :refer [px rem* fr em]]
            [tornado.colors :as colors]))

(def SimpleAtMedia (stylesheet/at-media {:min-width (px 500)
                                         :max-width (rem* 30)}
                                        [:#abc :.def {:margin-top       [[(px 15) 0 (fr 1) (px 25)]]
                                                      :opacity          0.5
                                                      :background-color (colors/hsla 100 33 67)}
                                         [:table {:width (em 10)}]]
                                        [:.ghi {:display :flex}]))