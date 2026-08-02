
# Daily APOD Space Wallpaper

> [!WARNING]
> This repo is no longer actively developed, but PRs will be merged!

## Building

Check out the repo and build — no other setup is required.

The app calls NASA's APOD API, and falls back to their shared `DEMO_KEY` if you have no key of
your own. That is rate limited across everyone using it, so for anything beyond a smoke test
[get your own key](https://api.nasa.gov/index.html#apply-for-an-api-key) and add it as
`APOD_API_KEY=xxxx` to `~/.gradle/gradle.properties`, outside the repo. A key can also be set at
runtime, in the app's own settings.

## Libraries
External libraries used in this app are listed below. Core Android / AndroidX libraries are excluded from this list.

* [OkHttp](https://github.com/square/okhttp) & [Gson](https://github.com/google/gson) (for networking)
* [Zoomage](https://github.com/jsibbold/zoomage) (for image zooming)
* [Material DateTime Picker](https://github.com/wdullaer/MaterialDateTimePicker) (for day picking)
* [RxJava](https://github.com/ReactiveX/RxJava) & [RxAndroid](https://github.com/ReactiveX/RxAndroid) (for threading)
* [Timber](https://github.com/JakeWharton/timber) (for logging)