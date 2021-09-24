# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

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
Added more flexibility to colors (e.g. :font-black, :fontblack, "font-black" "fontblack", 'font-black, 'fontblack) can all represent the same color.
Removed an information in README about upcoming lein-tornado plugin - the plugin is already released.

## 0.1.2
Fixed a bug where colors such as rgb did not have the r, g, b values rounded properly (e.g. 154.837256 instead of 155).
It caused that the colors did not work.

## 0.1.1
Removed lein-tornado plugin dependency.

## 0.1.0
The initial release of the Tornado library.