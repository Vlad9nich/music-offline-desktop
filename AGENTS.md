# YaNeoDex Desktop Project Context

## Overview

- Repository: standalone Windows-first desktop client for the YaNeoDex ecosystem.
- Root modules:
  - `shared-core`
  - `desktop-app`
- This repository is intentionally separate from the Android app and OCR backend repository.

## Product Goal

- Build a real Windows desktop MVP for local offline music playback.
- Avoid demo-only behavior in the main user flows.
- Keep reusable queue/import/parser contracts in `shared-core`.

## Current MVP Capabilities

- scans local music folders from disk
- extracts title, artist, and duration metadata
- persists desktop state and playlist state locally
- plays local media via JavaFX Media backend
- supports queue playback and shuffle
- searches remote parser source (`Ligaudio`)
- resolves parser candidates to direct media URLs
- sends screenshots to OCR backend
- polls OCR batch jobs and maps OCR candidates to local library matches
- supports playlist create, rename, add-track, remove-track

## Important Areas

### Shared Core

- contracts:
  - `shared-core/src/commonMain/kotlin/com/yaneodex/core/contracts/CoreContracts.kt`
- queue/shuffle logic:
  - `shared-core/src/commonMain/kotlin/com/yaneodex/core/playback/QueueShuffle.kt`
- OCR matching:
  - `shared-core/src/commonMain/kotlin/com/yaneodex/core/importer/ScreenshotImportMatcher.kt`
- UI state models:
  - `shared-core/src/commonMain/kotlin/com/yaneodex/core/state/DesktopState.kt`

### Desktop App

- app orchestration:
  - `desktop-app/src/jvmMain/kotlin/com/yaneodex/desktop/app/DesktopController.kt`
- Compose entry:
  - `desktop-app/src/jvmMain/kotlin/com/yaneodex/desktop/app/DesktopApp.kt`
  - `desktop-app/src/jvmMain/kotlin/com/yaneodex/desktop/app/Main.kt`
- main UI:
  - `desktop-app/src/jvmMain/kotlin/com/yaneodex/desktop/ui/MusicDesktopApp.kt`
- local library repository:
  - `desktop-app/src/jvmMain/kotlin/com/yaneodex/desktop/integration/DesktopLibraryRepository.kt`
- desktop persistence:
  - `desktop-app/src/jvmMain/kotlin/com/yaneodex/desktop/integration/DesktopPersistence.kt`
- playback backend:
  - `desktop-app/src/jvmMain/kotlin/com/yaneodex/desktop/integration/JavaFxPlaybackBackend.kt`
- parser integration:
  - `desktop-app/src/jvmMain/kotlin/com/yaneodex/desktop/integration/DesktopMusicSources.kt`
- OCR client:
  - `desktop-app/src/jvmMain/kotlin/com/yaneodex/desktop/integration/WindowsOcrClient.kt`
- desktop config:
  - `desktop-app/src/jvmMain/kotlin/com/yaneodex/desktop/integration/DesktopConfig.kt`

## Runtime Configuration

- Optional `.env` values:
  - `YANEODEX_LIBRARY_PATH`
  - `YANEODEX_OCR_BASE_URL`
  - `YANEODEX_OCR_TOKEN`
  - `YANEODEX_DOWNLOAD_DIR`
- Local persistent app state:
  - `%USERPROFILE%\.yaneodex-desktop\library.json`
  - `%USERPROFILE%\.yaneodex-desktop\state.json`

## Build And Test

- fast iteration:
  - `.\scripts\test.ps1`
  - `.\scripts\build.ps1`
  - `.\scripts\run.ps1`
- packaging:
  - `.\scripts\package.ps1`
- smoke checklist:
  - `.\scripts\smoke.ps1`

## Current Constraints

- Packaging on Windows is slow because Compose native distribution builds runtime image and installer tooling.
- Playback uses JavaFX Media, so codec support depends on Windows media/runtime support.
- OCR backend must remain compatible with the existing FastAPI contract:
  - `candidateId`
  - `screenshotIndex`
  - `rawText`
  - `artistGuess`
  - `titleGuess`
  - `confidence`
  - `bbox`

## Guidance For Future AI Sessions

- Prefer quick loop:
  - code
  - `:shared-core:jvmTest`
  - `:desktop-app:test`
  - `:desktop-app:build`
- Do not run packaging tasks on every small change.
- Treat this repository as the new desktop codebase; do not mix Android-specific assumptions here.
- Keep OCR and parser contracts backward-compatible unless explicitly changing both sides.
- Avoid reintroducing demo-only state into the primary controller flows.
