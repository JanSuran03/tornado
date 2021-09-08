(ns tornado.test-hiccup
  (:require [clojure.test :refer :all]
            [tornado.at-rules :as stylesheet]
            [tornado.core :as u]
            [tornado.colors :as colors]
            [tornado.at-rules :as at-rules]))

(def SimpleAtMedia (stylesheet/at-media {:min-width (u/px 500)
                                         :max-width (u/rem* 30)}
                                        [:#abc :.def {:margin-top       [[(u/px 15) 0 (u/fr 1) (u/px 25)]]
                                                      :opacity          0.5
                                                      :background-color (colors/hsla 100 33 67)}
                                         [:table {:width (u/em 10)}]]
                                        [:.ghi {:display :flex}]))

(def styles
  (list
    (list
      [:.abc :#def {:width      (u/px 15)
                    :height     (u/percent 20)
                    :margin-top [[(u/px 15) 0 (u/px 20) (u/rem* 3)]]}
       [:.ghi :#jkl {:height (u/fr 15)}]
       [:.mno {:height           (u/px 20)
               :background-color :chocolate}
        [:.pqr :#stu {:height (u/vw 25)
                      :width  (u/vh 20)}
         [:.vwx :.yza {:width nil}]]
        (at-rules/at-media {:min-width "500px"
                            :max-width "700px"}
                           [:.abc :#def {:margin-top [[0 "15px" "3rem" "1fr"]]}]
                           [:.ghi {:margin "20px"}
                            [:.jkl {:margin "150pc"}]]
                           [:.mno {:overflow :hidden}])]]
      [:.something :#something-else :#more-examples! {:width  (u/percent 15)
                                                      :height (u/percent 25)}])))