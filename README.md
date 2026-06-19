# OpenAudio Android

OpenAudio is a Kotlin + Jetpack Compose Android music player prototype focused on legal offline listening.

It supports:

- local audio library playback;
- search providers for open-license music catalogs;
- saving downloadable open-license tracks to local storage;
- a provider boundary for verified public-domain or Creative Commons torrent catalogs.

The app intentionally does not include piracy indexers or generic torrent-site scraping. Torrent providers should only return audio that is public domain, Creative Commons, or otherwise authorized for redistribution.

## Tech

- Kotlin
- Jetpack Compose
- AndroidX Media3 ExoPlayer
- Ktor HTTP client

## Open In Android Studio

Open this folder in Android Studio:

```text
open-audio-android
```

Android Studio will download Gradle and Android dependencies automatically.

## Provider Model

Search providers implement `MusicSourceProvider`. Each result carries license metadata and a source URL. Direct HTTP downloads are handled by Android `DownloadManager`. Torrent support is isolated behind provider metadata so only verified open catalogs should be connected.
