
# Daily APOD Wallpaper

## Building

1. Check out the repo.
2. [Create a Firebase project](https://console.firebase.google.com/u/0/) (for crash reporting).
3. Put your `google-services.json` file in the repository root. 

### APOD

1. [Get an APOD api key](https://api.nasa.gov/index.html#apply-for-an-api-key).
2. Add it as `apod_api_key="xxxx"` in your `gradle.properties`.

## Adding a new flavour

1. Add the new flavour to app-level `build.gradle`, along with any auth code needed.
2. Create a new Firebase project, download the `google-services.json` file, and place it in `/app/src/FLAVOURNAME/`.
3. Copy an existing `Config.kt`, and place it in `/app/src/FLAVOURNAME/java/PACKAGENAME/Config/`.

## Libraries
External libraries used in this app are listed below. Core Android / AndroidX libraries are excluded from this list.

* [Firebase Crashlytics](https://firebase.google.com/docs/crashlytics/get-started#android) (for crash reporting)
* [Firebase JobDispatcher](https://github.com/firebase/firebase-jobdispatcher-android) (for task scheduling)
* [OkHttp](https://github.com/square/okhttp) & [Gson](https://github.com/google/gson) (for networking)
* [Zoomage](https://github.com/jsibbold/zoomage) (for image zooming)
* [Material DateTime Picker](https://github.com/wdullaer/MaterialDateTimePicker) (for day picking)
* [RxJava](https://github.com/ReactiveX/RxJava) & [RxAndroid](https://github.com/ReactiveX/RxAndroid) (for threading)
* [Timber](https://github.com/JakeWharton/timber) (for logging)