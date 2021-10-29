# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## 0.2.5
**Bugfix**: Fixed hsl color compilation with pretty-print? set to false.

Functions on a few places replaced with dynamic bindings (*indent*, *compress?*).

## 0.2.4
Added a function `compile-params` for more convenient description of CSS styles in ClojureScript (Reagent).

Added functions `with-hue`, `with-saturation`, `with-lightness`, `with-alpha` you can use if you wanted to change e.g. only lightness of a color
(e.g. `(with-hue (rgb 50 100 150) 120)`, `(with-lightness "#FF0000" 0.3)`, `(with-alpha :black 0.5)`).

## 0.2.3
Fixed rgba color compilation. Added color compression to hex-format if `:pretty-print?` flag is set to false.

## 0.2.2
There was explicit java `Math` class on a few places - replaced with reader conditionals for java's `Math` and javascript's `js/Math` respectively.

Fixed mixing keyword, string or symbol colors which can be found in `tornado.colors/default-colors`.

## 0.2.1
`@rules` in one of the documentations replaced with at-rules to avoid collisions with the Closure compiler trying to parse it somehow.

# 0.2.0 ClojureScript support
Added support for ClojureScript. There are a few limitations that you have to require macros from a different file etc.

## 0.1.8
Removed duplicate selectors, added information to selectors in general.

## 0.1.7
Tiny improvements in code, removed a few printlines.

## 0.1.6
Some broken version, use **0.1.7** instead.

## 0.1.5
Small improvements in code.

## 0.1.4
Small improvements in code, updated README with plans for the future.

## 0.1.3
Added more flexibility to colors (e.g. `:font-black`, `:fontblack`, `"font-black"` `"fontblack"`, `'font-black`, `'fontblack`) can all represent the same color.

Removed an information in README about upcoming lein-tornado plugin - the plugin is already released.

## 0.1.2
Fixed a bug where colors such as rgb did not have the r, g, b values rounded properly (e.g. 154.837256 instead of 155).
It caused that the colors did not work.

## 0.1.1
Removed lein-tornado plugin dependency.

## 0.1.0
The initial release of the Tornado library.