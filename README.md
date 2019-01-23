
# Daily APOD Wallpaper

## Building

1. Check out the repo.
2. [Get an APOD api key](https://api.nasa.gov/index.html#apply-for-an-api-key).
3. Add it as `apod_api_key="xxxx"` in your `gradle.properties`.
4. [Create a Firebase project](https://console.firebase.google.com/u/0/) (for crash reporting).
5. Put your `google-services.json` file in the repository root. 

## Libraries
External libraries used in this app are listed below. Core Android / AndroidX libraries are excluded from this list.

* [Firebase Crashlytics](https://firebase.google.com/docs/crashlytics/get-started#android) (for crash reporting)
* [Firebase JobDispatcher](https://github.com/firebase/firebase-jobdispatcher-android) (for task scheduling)
* [OkHttp](https://github.com/square/okhttp) & [Gson](https://github.com/google/gson) (for networking)
* [Zoomage](https://github.com/jsibbold/zoomage) (for image zooming)
* [Material DateTime Picker](https://github.com/wdullaer/MaterialDateTimePicker) (for day picking)
* [RxJava](https://github.com/ReactiveX/RxJava) & [RxAndroid](https://github.com/ReactiveX/RxAndroid) (for threading)
* [Timber](https://github.com/JakeWharton/timber) (for logging)