# Kotlin-Activity-Tracker

## Music Feature
Since MediaStore.Audio.Playlists is [deprecated](https://developer.android.com/reference/android/provider/MediaStore.Audio.Playlists), The play music function play music files in `/Music`, and supports only `Flac` and `MP3` MIME types.

## Map Feature
A Google Maps API key is required to run the project. 
This project uses the [Secrets Gradle Plugin for Android](https://github.com/google/secrets-gradle-plugin) to hide the API key in the properties file.
Please place your API key in `local.properties` file like this:
```
MAPS_API_KEY=YOUR_API_KEY
```
