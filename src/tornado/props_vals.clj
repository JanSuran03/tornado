(ns tornado.props-vals)

(def css-properties
  "A set of all supported css properties."
  #{:align-content :align-items :align-self :all :animation :animation-delay :animation-direction :animation-duration :animation-fill-mode
    :animation-iteration-count :animation-name :animation-play-state :animation-timing-function :backface-visibility :background :background-attachment
    :background-clip :background-color :background-image :background-origin :background-repeat :background-size :border
    :border-bottom :border-bottom-color :border-bottom-left-radius :border-bottom-right-radius :border-radius :border-top-left-radius :border-top-right-radius :box-shadow
    :box-sizing :color :column-gap :cursor :display :flex-wrap :font :font-family :font-size :font-weight :gap :grid-area :grid-column-gap
    :grid-gap :grid-row-gap :grid-template-areas :grid-template-columns :grid-template-rows :height :justify-content :justify-items
    :justify-self :max-height :max-width :min-height :margin :margin-bottom :margin-left :margin-right :margin-top :min-width :opacity :order
    :overflow :overflow-x :overflow-y :padding :padding-bottom :padding-left :padding-tight :padding-top :position :row-gap :shadow :transition
    :width :z-index})

(def css-values
  "A set of all supported css values."
  #{:alternate :alternate-reverse  :auto :absolute :backwards :baseline :block :bold :border-box :both :bottom
    :center :color-dodge :column :column-reverse :contain :content-box :cover :darken :ease :ease-in :ease-in-out :ease-out :fixed :flex :flex-end :flex-start :forwards
    :grid :hidden :infinite :inherit :initial :left :lighten
    :linear :local :luminosity :monospace :multiply :none :no-repeat :normal :nowrap :overlay :padding-box :paused :pointer :relative :repeat :repeat-x :repeat-y:reverse :right :round
    :running :sans-serif :saturation :scroll :serif :solid :space :space-around :space-evenly
    :space-between :static :step-end :step-start :stretch :top :transparent :visible :wrap})

(comment :border-bottom-color)