
# Daily APOD Space Wallpaper

> [!WARNING]
> This repo is no longer actively developed, but PRs will be merged!

## Building

Check out the repo and build — no other setup is required.

The app reads NASA's [APOD endpoint](https://science.nasa.gov/wp-json/wp/v2/apod-basic), which
needs no API key. It replaced `api.nasa.gov/planetary/apod` in Sep 2026; see
[nasa/apod-api](https://github.com/nasa/apod-api).

## Libraries
External libraries used in this app are listed below. Core Android / AndroidX libraries are excluded from this list.

* [OkHttp](https://github.com/square/okhttp) & [Gson](https://github.com/google/gson) (for networking)
* [Zoomage](https://github.com/jsibbold/zoomage) (for image zooming)
* [Material DateTime Picker](https://github.com/wdullaer/MaterialDateTimePicker) (for day picking)
* [RxJava](https://github.com/ReactiveX/RxJava) & [RxAndroid](https://github.com/ReactiveX/RxAndroid) (for threading)
* [Timber](https://github.com/JakeWharton/timber) (for logging)