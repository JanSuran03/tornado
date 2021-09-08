(ns tornado.test-hiccup
  (:require [clojure.test :refer :all]
            [tornado.at-rules :as stylesheet]
            [tornado.core :as c :refer [px rem* percent fr em vw vh]]
            [tornado.colors :as colors]
            [tornado.at-rules :as at-rules]))

(def SimpleAtMedia (stylesheet/at-media {:min-width (px 500)
                                         :max-width (rem* 30)}
                                        [:#abc :.def {:margin-top       [[(px 15) 0 (fr 1) (px 25)]]
                                                      :opacity          0.5
                                                      :background-color (colors/hsla 100 33 67)}
                                         [:table {:width (em 10)}]]
                                        [:.ghi {:display :flex}]))

(def styles (list
              (list
                [:.abc :#def {:width      (px 15)
                              :height     (percent 20)
                              :margin-top [[(px 15) 0 (px 20) (rem* 3)]]}
                 [:.ghi :#jkl {:height (fr 15)}]
                 [:.mno {:height           (px 20)
                         :background-color :chocolate}
                  [:.pqr :#stu {:height (vw 25)
                                :width  (vh 20)}
                   [:.vwx :yza {:width nil}]]
                  (at-rules/at-media {:min-width "500px"
                                      :max-width "700px"}
                                     [:.abc :#def {:margin-top [[0 "15px" "3rem" "1fr"]]}]
                                     [:.ghi {:margin "20px"}
                                      [:.jkl {:margin "150pc"}]]
                                     [:.mno {:overflow :hidden}])]]
                [:.something :#something-else :#more-examples! {:width  (percent 15)
                                                                :height (percent 25)}])))