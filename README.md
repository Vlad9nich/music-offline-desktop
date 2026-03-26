# YaNeoDex Desktop MVP (Windows)

Standalone Windows-first desktop repository for YaNeoDex.

## What is implemented

- `shared-core` module:
  - core models for library/source/import/playback
  - queue+shuffle logic
  - OCR matching logic
  - stable contracts for:
    - `LibraryRepository`
    - `PlaybackBackend`
    - `MusicSourceCatalog` / `MusicSource`
    - `OcrImportClient`
- `desktop-app` module:
  - Compose Desktop UI with Spotify-inspired desktop composition
  - real local library scan from selected disk folders
  - metadata extraction (title/artist/duration) from audio files
  - real Windows playback backend (JavaFX Media)
  - remote parser search/resolve (Ligaudio)
  - OCR playlist import (`/v1/ocr-image`, `/v1/playlist-import/jobs/...`)
  - playlist create/rename/add/remove flows
  - RU default UI with RU/EN language switch
  - state persistence to `%USERPROFILE%\.yaneodex-desktop`

## Tech stack

- Kotlin 2.3.0
- Compose Desktop 1.10.0
- JVM 21
- Jsoup (parser)
- JavaFX Media (playback)
- jaudiotagger (audio metadata)

## Project layout

- `shared-core/` shared domain/contracts/use-cases
- `desktop-app/` Windows UI + adapters + DI wiring
- `scripts/` build/test/run/package/smoke scripts
- `.env.example` config template for OCR/library defaults

## Quickstart (Windows)

End users do not need to install Java separately when using the packaged MSI or EXE.

1. Install JDK 21 only if you want to run or build the project from source.
2. Copy `.env.example` to `.env` and set values as needed.
3. Run app:

```powershell
.\scripts\run.ps1
```

Scripts auto-detect JDK 21 and will also use Android Studio bundled `jbr` when it is available locally.
Installer packaging still requires a full JDK 21 with `jpackage.exe`, but the packaged app itself should launch with its bundled runtime and must not require a separate Java install on the user's machine.

## Build/Test/Package profiles

```powershell
.\scripts\build.ps1
.\scripts\test.ps1
.\scripts\package.ps1
.\scripts\smoke.ps1
```

Packaging outputs:
- portable distribution: `desktop-app/build/compose/binaries/main-release/app/`
- installers: MSI/EXE under `desktop-app/build/compose/binaries/main-release/`

## OCR contract

Desktop client expects OCR candidates in this shape:

```json
{
  "candidateId": "string",
  "screenshotIndex": 0,
  "rawText": "Title | Artist",
  "artistGuess": "Artist",
  "titleGuess": "Title",
  "confidence": 0.0,
  "bbox": [0, 0, 100, 50]
}
```

## Troubleshooting

- `Build fails on JDK 25+`:
  - use the provided `scripts/*.ps1`; they switch to JDK 21 automatically when possible.
- `package.ps1` stops before Gradle starts:
  - install a full JDK 21 with `jpackage.exe`; Android Studio bundled `jbr` is enough for build/test/run, but not for MSI/EXE packaging.
- installed app shows a launch error about incomplete runtime:
  - reinstall using the latest MSI/EXE; the packaged app should already contain everything needed to run.
- `SDK location not found`:
  - This desktop repo does not need Android SDK; run commands from this repository only.
- `Playback failed`:
  - check file path accessibility and codec support in Windows Media stack.
- `OCR HTTP 401/429`:
  - verify bearer token and backend rate limits.
- `No tracks found`:
  - add folders via `Library -> Add folders` and refresh.
