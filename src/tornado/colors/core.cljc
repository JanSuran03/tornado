(ns tornado.colors.core
  (:require [tornado.colors.util :as color-util]
            [tornado.colors.conversion :as conversion]))

;; tests - will be properly moved to the test module namespaces later
(defmacro with-err-out [& body]
  `(binding [*out* *err*]
     ~@body))

(defmacro test-one
  "All the color tests be moved to the test namespaces after being ready to replace
  the older API, now kept here for simplicity and ease of development"
  [test-name & body]
  `(let [gres# (try ~@body
                    (catch Throwable t#
                      t#))
         gfail# ~(apply str "Failed on test case: " test-name ", form: " body)]
     (if (or (not gres#) (instance? Throwable gres#))
       (with-err-out (println (str gfail#
                                   (when gres#
                                     (str ", reason: " (.getMessage gres#)))))))))

(defmacro test-multiple
  [test-name & forms]
  `(do ~@(map-indexed (fn [i form]
                        `(test-one ~(keyword (str (name test-name) "-" (inc i)))
                                   ~form))
                      forms)))

(defmacro expect-throw [& body]
  `(try ~@body
        nil
        (catch Throwable _# true)))

(defmacro TODO
  ([expr] `(TODO nil ~expr))
  ([need-todo? expr]
   (if need-todo?
     `(with-err-out
        (println "Need to resolve TODO: " ~(str expr))
        true)
     `(with-err-out
        (let [res# ~expr]
          (if res#
            (do (println "TODO not needed:" ~(str expr))
                res#)
            (do (print "TODO: ")
                nil)))))))



(test-multiple :type-of-color
  (= (color-util/type-of-color {:type :color/rgb :red 20 :blue 40 :green 60}) :color/rgb)
  (= (color-util/type-of-color {:red 20 :green 40 :blue 60}) :color/rgb)
  (nil? (color-util/type-of-color {:red 20 :green 40 :magenta 60}))
  (nil? (color-util/type-of-color {:type :unit/px :red 20 :blue 40 :green 60}))
  (nil? (color-util/type-of-color 42))
  (= (color-util/type-of-color :light-blue) :color/literal)
  (= (color-util/type-of-color 'light-blue) :color/literal)
  (= (color-util/type-of-color "light-blue") :color/literal)
  (= (color-util/type-of-color :lightblue) :color/literal)
  (= (color-util/type-of-color 'lightblue) :color/literal)
  (= (color-util/type-of-color "lightblue") :color/literal)
  (TODO :force (nil? (color-util/type-of-color :not-a-color)))
  (TODO :force (nil? (color-util/type-of-color 'not-a-color)))
  (TODO :force (nil? (color-util/type-of-color "not-a-color")))
  (TODO :force (nil? (color-util/type-of-color "#12345")))
  (TODO :force (= (color-util/type-of-color "#123456") :color/hex))
  (TODO :force (nil? (color-util/type-of-color "#1234567")))
  (TODO :force (= (color-util/type-of-color "#12345678") :color/hex))
  (TODO :force (nil? (color-util/type-of-color "#123456789")))
  (TODO :force (nil? (color-util/type-of-color "#12345p")))
  (TODO :force (nil? (color-util/type-of-color "#1234567p")))
  (TODO :force (nil? (color-util/type-of-color "123456")))
  (TODO :force (nil? (color-util/type-of-color "1234567")))
  (TODO :force (nil? (color-util/type-of-color "12345678")))
  (TODO :force (nil? (color-util/type-of-color "123456789")))
  (= (conversion/convert {:red 20 :green 40 :blue 60} :color/rgb)
     {:red 20 :green 40 :blue 60})
  (= (conversion/convert {:red 20 :green 40 :blue 60 :alpha 0.3} :color/rgba)
     {:red 20 :green 40 :blue 60 :alpha 0.3})
  (= (conversion/convert {:hue 20 :saturation 0.5 :lightness 0.7} :color/hsl)
     {:hue 20 :saturation 0.5 :lightness 0.7})
  (= (conversion/convert {:hue 20 :saturation 0.5 :lightness 0.7 :alpha 0.3} :color/hsla)
     {:hue 20 :saturation 0.5 :lightness 0.7 :alpha 0.3})
  (TODO (= (conversion/convert "#123456" :color/hex) "#123456"))
  (TODO (= (conversion/convert "#123456" :color/hexa) "#123456ff"))
  (TODO (= (conversion/convert "#12345678" :color/hex) "#123456"))
  (TODO (= (conversion/convert "#12345678" :color/hexa) "#12345678")))
