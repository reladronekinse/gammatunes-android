# GammaTunes

An unofficial Android client for YouTube Music, with SoundCloud as a second
search source.

The app ships with a small backend that runs directly on the device (via
[Chaquopy](https://chaquo.com/chaquopy/)) to talk to YouTube Music and
SoundCloud, so no separate server is required to use the app. A standalone
version of the same backend is also included under `backend/`, in case you'd
rather run it on a PC or server instead of on-device.

## Project structure

```
android/    Android client (Kotlin, Jetpack Compose)
backend/    Standalone FastAPI backend (optional, PC/server use)
```

## Building

### Android app

Requirements: Android Studio (or the command-line Android SDK/build-tools),
a JDK 17, and Python 3.11 on the **build machine** (needed by the Chaquopy
plugin to resolve `ytmusicapi` / `yt-dlp` into the APK). No Python install is
required on the phone.

```bash
cd android
./gradlew assembleDebug
```

The resulting APK will be at
`android/app/build/outputs/apk/debug/app-debug.apk`.

## Features (0.4-stable)

- Embedded Python backend (ytmusicapi + yt-dlp) via Chaquopy
- Material 3 UI with Liquid Glass surfaces
- Home tab with personalised recommendations mixing YouTube Music radio (from your
  top/recent tracks) and SoundCloud picks (by favourite artists), recently played and top tracks
- Search as you type (debounced) with persistent search history (online and
  downloaded-tracks searches keep separate histories)
- Search (artists + tracks), artist/album detail
- Source switcher in search: YouTube Music or SoundCloud (tracks and artists;
  progressive and HLS streams are supported for playback and offline downloads)
- Playback via ExoPlayer + MediaSession foreground service (notification controls)
- Queue screen (open from the player): jump to a track, drag to reorder, remove;
  "Play next" button on track rows and tiles
- Queue next/previous, repeat modes, seek
- Offline downloads (tracks and full albums), searchable cached-tracks list
- Offline mode: offered automatically when the connection drops; search, home and
  next/previous then use downloaded tracks only
- Browser-header login for YouTube Music likes and library playlists
- SoundCloud account: in-app browser sign-in (or paste an `oauth_token`),
  likes, your/liked playlists, add-to-playlist, artist pages (popular tracks,
  albums, playlists)
- Account screen with a YouTube Music / SoundCloud switch (liked songs, playlists)
- Appearance settings (cover style, seek bar, accents)
- EN / RU localization

### SoundCloud account notes

SoundCloud's official API requires a registered developer app, so the app uses
the same private `api-v2` endpoints soundcloud.com uses, authenticated with the
web session's `oauth_token` cookie. This is unofficial and can break whenever
SoundCloud changes its site. E-mail sign-in works in the embedded browser;
Google/Apple/Facebook buttons usually don't (pop-ups) — use *Sign in with token*
and paste the `oauth_token` cookie value from soundcloud.com instead.

The SoundCloud account code lives in
`android/app/src/main/python/sc_account.py`. Offline tests (fake SoundCloud, no
network):

```bash
python3 -m unittest discover -s android/tests -v
```

## Acknowledgments

GammaTunes builds on the work of several open-source projects:

- [ytmusicapi](https://github.com/sigma67/ytmusicapi) — unofficial YouTube
  Music API used for search, artist, and album metadata.
- [yt-dlp](https://github.com/yt-dlp/yt-dlp) — used to resolve playable audio
  stream URLs and to search/stream SoundCloud.
- [FastAPI](https://github.com/fastapi/fastapi), [Uvicorn](https://github.com/encode/uvicorn),
  and [Pydantic](https://github.com/pydantic/pydantic) — power the standalone
  backend.
- [Chaquopy](https://chaquo.com/chaquopy/) — embeds a Python interpreter in
  the Android app so the backend can run on-device.
- [Jetpack Compose](https://developer.android.com/jetpack/compose) and the
  AndroidX libraries (Lifecycle, Navigation, Media3/ExoPlayer) — the app's UI
  toolkit and media playback stack.
- [Retrofit](https://github.com/square/retrofit) and [OkHttp](https://github.com/square/okhttp)
  (Square) — networking.
- [Coil](https://github.com/coil-kt/coil) — image loading in Compose.
- [Gson](https://github.com/google/gson) — JSON parsing (via Retrofit's
  converter).

This is an unofficial, community project and is not affiliated with,
endorsed by, or sponsored by Google, YouTube, or SoundCloud.

## License

Licensed under the Apache License, Version 2.0 — see [LICENSE](LICENSE) for
the full text.
