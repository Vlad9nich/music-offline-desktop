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
  - `buildPlaybackQueue` (pins the current track, artist-aware spread)
  - `reshufflePlaybackQueue` (fresh cycle when the queue runs out)
  - `unshufflePlaybackQueue` (restore source order without moving the current track)
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

## Playback And UI Invariants

These are load-bearing; breaking them is what caused the desync bugs fixed in the redesign.

- The queue order lives in exactly one place. Any code that changes `playbackQueue` must push the
  same order to the player — use `DesktopController.mutate` (which flushes `pendingBackendQueue`)
  or call `PlaybackBackend.setQueue` explicitly. `setQueue` swaps the order **without** restarting
  the current track; `playQueue` restarts playback and is only for starting a new queue.
- `PlaybackSnapshot.queueExhausted` is a request for a decision, not an end state. The controller
  answers it in `continueAfterQueueEnd()` (reshuffle when shuffled, wrap around otherwise).
- The visualizer is smoothed in the renderer, not in the controller. `PlaybackVisualizerState`
  carries raw targets at ~30 fps; the canvas eases them on the display frame clock. Band count is
  always `VISUALIZER_BANDS` (32). `spectrumLive = false` means the bars are a generated fallback
  for a codec whose spectrum JavaFX reports as flat — surface that honestly, never silently fake it.
- Design tokens live in `ui/theme/YaNeoDexDesktopTheme.kt`. Depth comes from surface lightness
  (`Bg` -> `Panel` -> `PanelRaised` -> `PanelHover`); hairlines are low-contrast by design. Use
  `Wd2Fonts.Content` for language, `Wd2Fonts.Meta` for counters/timers, and `Wd2.Accent` only for
  active state and primary actions. Radii come from `Wd2Radius`.
- Avoid full-screen animated overlays. The CRT layer is intentionally static; an always-running
  full-window repaint is a measurable cost and was the main source of the harsh look.
- Icons come from `ui/theme/YaNeoDexIcons.kt` (`YdxGlyph` + `YdxIcon`), not from Material. The
  whole set is drawn on a 24x24 grid with one stroke weight, round caps and round joins, so
  nothing mixes filled and outlined weights. Do not add a `material-icons-extended` glyph back in:
  at 18-22dp the stock set turns to mush and its fill weights disagree with each other.
- Sizes are deliberate: 22dp in sidebar rows and transport buttons, 26dp for the primary play
  button, 20dp for top-bar chrome, 18-19dp inside chips. `stroke` defaults to 7.5% of the box, so
  optical weight stays constant if you resize a glyph.
- The logo is `YaNeoDexMark` (three level bars, tallest in accent) plus a wide-tracked wordmark.
  The window/taskbar icon in `app/Main.kt` draws the same mark with Java2D — if you change one,
  change the other.
- The main window sets its own size and minimum size in `app/Main.kt`. Without it Compose opens at
  800x600 and the sidebar, main column and queue rail crush into each other.
