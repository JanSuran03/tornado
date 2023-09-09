(ns tornado.colors2
  (:require [tornado.util :as util]))

(def rgb-schema #{:red :green :blue})
(def rgba-schema (conj rgb-schema :alpha))
(def hsl-schema #{:hue :saturation :lightness})
(def hsla-schema (conj hsl-schema :alpha))

; TODO: extending with custom types?
(def schema->type {rgb-schema  :color/rgb
                   rgba-schema :color/rgba
                   hsl-schema  :color/hsl
                   hsla-schema :color/hsla})

(defn- from-hex [color])

(defn- from-literal [color])
(def maybe-kw-type (util/make-css-type-checker "color"))

(defn type-of-color [color]
  (or (and (map? color)
           (or (maybe-kw-type color)
               (schema->type (set (keys color)))))
      (from-literal color)
      (from-hex color)))

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
                                     (str ", reason: " (.getMessage gfail#)))))))))

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
  (= (type-of-color {:type :color/rgb :red 20 :blue 40 :green 60}) :color/rgb)
  (= (type-of-color {:red 20 :green 40 :blue 60}) :color/rgb)
  (nil? (type-of-color {:red 20 :green 40 :magenta 60}))
  (nil? (type-of-color {:type :unit/px :red 20 :blue 40 :green 60}))
  (nil? (type-of-color 42))
  (TODO (= (type-of-color :light-blue) :color/literal))
  (TODO (= (type-of-color 'light-blue) :color/literal))
  (TODO (= (type-of-color "light-blue") :color/literal))
  (TODO (= (type-of-color :lightblue) :color/literal))
  (TODO (= (type-of-color 'lightblue) :color/literal))
  (TODO (= (type-of-color "lightblue") :color/literal))
  (TODO :force (nil? (type-of-color :not-a-color)))
  (TODO :force (nil? (type-of-color 'not-a-color)))
  (TODO :force (nil? (type-of-color "not-a-color")))
  (TODO :force (nil? (type-of-color "#12345")))
  (TODO :force (= (type-of-color "#123456") :color/hex))
  (TODO :force (nil? (type-of-color "#1234567")))
  (TODO :force (= (type-of-color "#12345678") :color/hex))
  (TODO :force (nil? (type-of-color "#123456789")))
  (TODO :force (nil? (type-of-color "#12345p")))
  (TODO :force (nil? (type-of-color "#1234567p")))
  (TODO :force (nil? (type-of-color "123456")))
  (TODO :force (nil? (type-of-color "1234567")))
  (TODO :force (nil? (type-of-color "12345678")))
  (TODO :force (nil? (type-of-color "123456789"))))
