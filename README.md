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
  - Compose Desktop UI with Spotify-like desktop composition
  - real local library scan from selected disk folders
  - metadata extraction (title/artist/duration) from audio files
  - real Windows playback backend (JavaFX Media)
  - remote parser search/resolve (Ligaudio)
  - OCR playlist import (`/v1/ocr-image`, `/v1/playlist-import/jobs/...`)
  - playlist create/rename/add/remove flows
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

1. Install JDK 21 and ensure `JAVA_HOME` points to it.
2. Copy `.env.example` to `.env` and set values as needed.
3. Run app:

```powershell
.\scripts\run.ps1
```

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

- `SDK location not found`:
  - This desktop repo does not need Android SDK; run commands from this repository only.
- `Playback failed`:
  - check file path accessibility and codec support in Windows Media stack.
- `OCR HTTP 401/429`:
  - verify bearer token and backend rate limits.
- `No tracks found`:
  - add folders via `Library -> Add folders` and refresh.
