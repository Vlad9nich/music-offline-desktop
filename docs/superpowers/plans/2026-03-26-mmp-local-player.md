# YaNeoDex Desktop MMP Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the current Windows desktop MVP into a Minimal Marketable Product: a stable local offline player with parser and OCR features that people can install, launch, and use without developer intervention.

**Architecture:** Keep `shared-core` as the source of truth for contracts, playback state, and import/parser domain models, while `desktop-app` owns Windows-specific playback, persistence, packaging, and Compose UI. Preserve the current navigation and section structure as a product constraint: do not redesign the left nav or tab model, only refine spacing, density, hierarchy, and interaction polish. The MMP work is split into reliability first, UX/productization second, and release operations third so each stage produces a usable build instead of a long-lived unstable branch.

**Tech Stack:** Kotlin 2.3, Compose Desktop 1.10, JVM 21, JavaFX Media, kotlinx serialization/coroutines, Jsoup/HTTP parser integration, jaudiotagger, PowerShell packaging scripts, MSI/EXE installers.

**Non-goals / UI guardrails:**
- do not significantly change the navigation model
- do not replace or restructure the existing top-level tabs/sections
- keep the current visual direction and just make it cleaner, denser, and more pleasant to look at

---

## File Map

**Core runtime and domain**
- Modify: `C:\Users\vladg\music-offline-desktop\shared-core\src\commonMain\kotlin\com\yaneodex\core\contracts\CoreContracts.kt`
- Modify: `C:\Users\vladg\music-offline-desktop\shared-core\src\commonMain\kotlin\com\yaneodex\core\state\DesktopState.kt`
- Modify: `C:\Users\vladg\music-offline-desktop\shared-core\src\commonMain\kotlin\com\yaneodex\core\state\DemoLibrary.kt`

**Desktop orchestration and integrations**
- Modify: `C:\Users\vladg\music-offline-desktop\desktop-app\src\jvmMain\kotlin\com\yaneodex\desktop\app\DesktopController.kt`
- Modify: `C:\Users\vladg\music-offline-desktop\desktop-app\src\jvmMain\kotlin\com\yaneodex\desktop\integration\JavaFxPlaybackBackend.kt`
- Modify: `C:\Users\vladg\music-offline-desktop\desktop-app\src\jvmMain\kotlin\com\yaneodex\desktop\integration\DesktopMusicSources.kt`
- Modify: `C:\Users\vladg\music-offline-desktop\desktop-app\src\jvmMain\kotlin\com\yaneodex\desktop\integration\DesktopLibraryRepository.kt`
- Modify: `C:\Users\vladg\music-offline-desktop\desktop-app\src\jvmMain\kotlin\com\yaneodex\desktop\integration\DesktopPersistence.kt`
- Modify: `C:\Users\vladg\music-offline-desktop\desktop-app\src\jvmMain\kotlin\com\yaneodex\desktop\integration\JavaFxRuntime.kt`
- Modify: `C:\Users\vladg\music-offline-desktop\desktop-app\src\jvmMain\kotlin\com\yaneodex\desktop\app\Main.kt`

**UI/product surface**
- Modify: `C:\Users\vladg\music-offline-desktop\desktop-app\src\jvmMain\kotlin\com\yaneodex\desktop\ui\MusicDesktopApp.kt`
- Modify: `C:\Users\vladg\music-offline-desktop\desktop-app\src\jvmMain\kotlin\com\yaneodex\desktop\ui\DesktopStrings.kt`

**Packaging/release**
- Modify: `C:\Users\vladg\music-offline-desktop\desktop-app\build.gradle.kts`
- Modify: `C:\Users\vladg\music-offline-desktop\scripts\package.ps1`
- Modify: `C:\Users\vladg\music-offline-desktop\scripts\smoke.ps1`
- Modify: `C:\Users\vladg\music-offline-desktop\README.md`

**Tests**
- Modify: `C:\Users\vladg\music-offline-desktop\desktop-app\src\test\kotlin\com\yaneodex\desktop\app\DesktopControllerTest.kt`
- Modify: `C:\Users\vladg\music-offline-desktop\desktop-app\src\test\kotlin\com\yaneodex\desktop\integration\DesktopLibraryRepositoryTest.kt`
- Create: `C:\Users\vladg\music-offline-desktop\desktop-app\src\test\kotlin\com\yaneodex\desktop\integration\JavaFxPlaybackBackendContractTest.kt`
- Create: `C:\Users\vladg\music-offline-desktop\desktop-app\src\test\kotlin\com\yaneodex\desktop\ui\DesktopLayoutStateTest.kt`

---

## Chunk 1: Reliability Floor

### Task 1: Lock playback into a stable desktop contract

**Files:**
- Modify: `C:\Users\vladg\music-offline-desktop\shared-core\src\commonMain\kotlin\com\yaneodex\core\contracts\CoreContracts.kt`
- Modify: `C:\Users\vladg\music-offline-desktop\shared-core\src\commonMain\kotlin\com\yaneodex\core\state\DesktopState.kt`
- Modify: `C:\Users\vladg\music-offline-desktop\desktop-app\src\jvmMain\kotlin\com\yaneodex\desktop\integration\JavaFxPlaybackBackend.kt`
- Modify: `C:\Users\vladg\music-offline-desktop\desktop-app\src\jvmMain\kotlin\com\yaneodex\desktop\app\DesktopController.kt`
- Test: `C:\Users\vladg\music-offline-desktop\desktop-app\src\test\kotlin\com\yaneodex\desktop\app\DesktopControllerTest.kt`

- [ ] **Step 1: Write failing tests for playback progress, stop/reset, and preview playback**

Add or extend tests to verify:
- `PlaybackSnapshot.positionMs` and `durationMs` propagate into `DesktopUiState`
- stop/error resets progress cleanly
- parser preview uses the same progress path as local tracks

- [ ] **Step 2: Run the focused desktop tests to verify failure**

Run: `.\gradlew.bat :desktop-app:test --tests "com.yaneodex.desktop.app.DesktopControllerTest" --console=plain`

- [ ] **Step 3: Implement minimal runtime contract changes**

Ensure:
- playback emits `currentTrackId`, `isPlaying`, `visualizer`, `positionMs`, `durationMs`
- controller resets progress on track changes and preserves it while same track continues

- [ ] **Step 4: Verify focused tests pass**

Run: `.\gradlew.bat :desktop-app:test --tests "com.yaneodex.desktop.app.DesktopControllerTest" --console=plain`

- [ ] **Step 5: Commit**

```bash
git add shared-core/src/commonMain/kotlin/com/yaneodex/core/contracts/CoreContracts.kt shared-core/src/commonMain/kotlin/com/yaneodex/core/state/DesktopState.kt desktop-app/src/jvmMain/kotlin/com/yaneodex/desktop/integration/JavaFxPlaybackBackend.kt desktop-app/src/jvmMain/kotlin/com/yaneodex/desktop/app/DesktopController.kt desktop-app/src/test/kotlin/com/yaneodex/desktop/app/DesktopControllerTest.kt
git commit -m "fix: stabilize desktop playback state contract"
```

### Task 2: Eliminate install-time playback/runtime regressions

**Files:**
- Modify: `C:\Users\vladg\music-offline-desktop\desktop-app\build.gradle.kts`
- Modify: `C:\Users\vladg\music-offline-desktop\desktop-app\src\jvmMain\kotlin\com\yaneodex\desktop\integration\JavaFxRuntime.kt`
- Modify: `C:\Users\vladg\music-offline-desktop\desktop-app\src\jvmMain\kotlin\com\yaneodex\desktop\app\Main.kt`
- Modify: `C:\Users\vladg\music-offline-desktop\scripts\package.ps1`
- Test: `C:\Users\vladg\music-offline-desktop\desktop-app\src\test\kotlin\com\yaneodex\desktop\integration\JavaFxPlaybackBackendContractTest.kt`

- [ ] **Step 1: Write failing packaging/runtime regression tests**

Cover:
- release packaging path does not strip required JavaFX runtime jars
- launcher path and backend path do not require incompatible early toolkit bootstrap

- [ ] **Step 2: Run packaging-related tests and reproduce the bug**

Run:
- `.\gradlew.bat :desktop-app:test --tests "com.yaneodex.desktop.integration.JavaFxPlaybackBackendContractTest" --console=plain`
- `.\scripts\package.ps1`

- [ ] **Step 3: Remove or replace brittle startup assumptions**

Target outcomes:
- no release path that shrinks JavaFX runtime incorrectly
- installer versioning/updating behaves predictably
- packaged app launches from `Program Files` without manual copying

- [ ] **Step 4: Verify portable app and MSI install path**

Run:
- `.\gradlew.bat :desktop-app:createDistributable --console=plain`
- `.\gradlew.bat :desktop-app:packageMsi --console=plain`
- install MSI manually and smoke launch from `C:\Program Files\YaNeoDex Desktop`

- [ ] **Step 5: Commit**

```bash
git add desktop-app/build.gradle.kts desktop-app/src/jvmMain/kotlin/com/yaneodex/desktop/integration/JavaFxRuntime.kt desktop-app/src/jvmMain/kotlin/com/yaneodex/desktop/app/Main.kt scripts/package.ps1 desktop-app/src/test/kotlin/com/yaneodex/desktop/integration/JavaFxPlaybackBackendContractTest.kt
git commit -m "fix: make packaged desktop playback launch reliably"
```

---

## Chunk 2: MMP Product UX

### Task 3: Finish the listening surface

**Files:**
- Modify: `C:\Users\vladg\music-offline-desktop\desktop-app\src\jvmMain\kotlin\com\yaneodex\desktop\ui\MusicDesktopApp.kt`
- Modify: `C:\Users\vladg\music-offline-desktop\desktop-app\src\jvmMain\kotlin\com\yaneodex\desktop\ui\DesktopStrings.kt`
- Test: `C:\Users\vladg\music-offline-desktop\desktop-app\src\test\kotlin\com\yaneodex\desktop\ui\DesktopLayoutStateTest.kt`

- [ ] **Step 1: Write failing UI state/layout tests**

Cover:
- single main visualizer in hero only
- bottom player shows progress timeline instead of duplicate visualizer
- section transitions do not overflow bounds
- compact widths do not overlap title/badge/action areas

- [ ] **Step 2: Run focused UI/state tests**

Run: `.\gradlew.bat :desktop-app:test --tests "com.yaneodex.desktop.ui.DesktopLayoutStateTest" --console=plain`

- [ ] **Step 3: Implement polished listening UI**

Requirements:
- one premium hero visualizer
- real progress bar with current/duration time
- progress timeline can be scrubbed with mouse and adjusted with keyboard arrows
- cleaner now-playing card
- responsive compact/wide layouts
- no duplicated explanatory copy
- remove the mood/tag buttons row (`Night Drive`, `Focused Code`, `Glass Pop`, `Soft Warehouse`, `Late Commute`) from the main surface entirely
- remove the non-clickable hero chips (`Parser`, `OCR`, `Windows`) from the hero area
- shrink the right-rail `Сейчас / Очередь / Перемешать / Парсер / OCR` elements so they are compact, minimal, and use much less vertical space

- [ ] **Step 4: Verify compile + test**

Run:
- `.\gradlew.bat :desktop-app:compileKotlin --console=plain`
- `.\gradlew.bat :desktop-app:test --tests "com.yaneodex.desktop.ui.DesktopLayoutStateTest" --console=plain`

- [ ] **Step 5: Commit**

```bash
git add desktop-app/src/jvmMain/kotlin/com/yaneodex/desktop/ui/MusicDesktopApp.kt desktop-app/src/jvmMain/kotlin/com/yaneodex/desktop/ui/DesktopStrings.kt desktop-app/src/test/kotlin/com/yaneodex/desktop/ui/DesktopLayoutStateTest.kt
git commit -m "feat: polish now-playing surface for desktop mmp"
```

### Task 3.1: Eliminate cross-tab layout drift and overlap bugs

**Files:**
- Modify: `C:\Users\vladg\music-offline-desktop\desktop-app\src\jvmMain\kotlin\com\yaneodex\desktop\ui\MusicDesktopApp.kt`
- Test: `C:\Users\vladg\music-offline-desktop\desktop-app\src\test\kotlin\com\yaneodex\desktop\ui\DesktopLayoutStateTest.kt`

- [ ] **Step 1: Write failing layout regression tests**

Cover:
- section transitions do not leak previous content into the next tab
- no left-edge clipping or overlapping badges/text in playlist/library/import/search surfaces
- no persistent decorative rows survive when section changes

- [ ] **Step 2: Run focused layout tests**

Run: `.\gradlew.bat :desktop-app:test --tests "com.yaneodex.desktop.ui.DesktopLayoutStateTest" --console=plain`

- [ ] **Step 3: Fix layout systemically, not screen-by-screen**

Requirements:
- transition container clipped to bounds
- section content aligned from a single top-left origin
- shared row/card components use one spacing model
- compact breakpoints do not produce badge/text overlap

- [ ] **Step 4: Verify compile + focused tests**

Run:
- `.\gradlew.bat :desktop-app:compileKotlin --console=plain`
- `.\gradlew.bat :desktop-app:test --tests "com.yaneodex.desktop.ui.DesktopLayoutStateTest" --console=plain`

- [ ] **Step 5: Commit**

```bash
git add desktop-app/src/jvmMain/kotlin/com/yaneodex/desktop/ui/MusicDesktopApp.kt desktop-app/src/test/kotlin/com/yaneodex/desktop/ui/DesktopLayoutStateTest.kt
git commit -m "fix: remove cross-tab layout drift in desktop ui"
```

### Task 4: Make parser and OCR flows feel productized, not dev-only

**Files:**
- Modify: `C:\Users\vladg\music-offline-desktop\desktop-app\src\jvmMain\kotlin\com\yaneodex\desktop\app\DesktopController.kt`
- Modify: `C:\Users\vladg\music-offline-desktop\desktop-app\src\jvmMain\kotlin\com\yaneodex\desktop\integration\DesktopMusicSources.kt`
- Modify: `C:\Users\vladg\music-offline-desktop\desktop-app\src\jvmMain\kotlin\com\yaneodex\desktop\ui\MusicDesktopApp.kt`
- Modify: `C:\Users\vladg\music-offline-desktop\desktop-app\src\jvmMain\kotlin\com\yaneodex\desktop\ui\DesktopStrings.kt`
- Test: `C:\Users\vladg\music-offline-desktop\desktop-app\src\test\kotlin\com\yaneodex\desktop\app\DesktopControllerTest.kt`

- [ ] **Step 1: Write failing tests for parser/OCR happy-path and recovery-path statuses**

Cover:
- parser preview/download/add-to-playlist statuses
- OCR missing config state
- OCR success/failure state mapping

- [ ] **Step 2: Run focused tests to verify failure**

Run: `.\gradlew.bat :desktop-app:test --tests "com.yaneodex.desktop.app.DesktopControllerTest" --console=plain`

- [ ] **Step 3: Implement product UX states**

Requirements:
- fewer raw file-path/status dumps
- explicit loading/success/failure state messages
- empty results states that feel intentional
- no technical phrasing exposed to end users

- [ ] **Step 4: Verify focused tests pass**

Run: `.\gradlew.bat :desktop-app:test --tests "com.yaneodex.desktop.app.DesktopControllerTest" --console=plain`

- [ ] **Step 5: Commit**

```bash
git add desktop-app/src/jvmMain/kotlin/com/yaneodex/desktop/app/DesktopController.kt desktop-app/src/jvmMain/kotlin/com/yaneodex/desktop/integration/DesktopMusicSources.kt desktop-app/src/jvmMain/kotlin/com/yaneodex/desktop/ui/MusicDesktopApp.kt desktop-app/src/jvmMain/kotlin/com/yaneodex/desktop/ui/DesktopStrings.kt desktop-app/src/test/kotlin/com/yaneodex/desktop/app/DesktopControllerTest.kt
git commit -m "feat: productize parser and ocr flows"
```

---

## Chunk 3: Daily-Use Local Player

### Task 5: Harden library management for everyday use

**Files:**
- Modify: `C:\Users\vladg\music-offline-desktop\desktop-app\src\jvmMain\kotlin\com\yaneodex\desktop\integration\DesktopLibraryRepository.kt`
- Modify: `C:\Users\vladg\music-offline-desktop\desktop-app\src\jvmMain\kotlin\com\yaneodex\desktop\integration\DesktopPersistence.kt`
- Modify: `C:\Users\vladg\music-offline-desktop\desktop-app\src\jvmMain\kotlin\com\yaneodex\desktop\app\DesktopController.kt`
- Test: `C:\Users\vladg\music-offline-desktop\desktop-app\src\test\kotlin\com\yaneodex\desktop\integration\DesktopLibraryRepositoryTest.kt`

- [ ] **Step 1: Write failing tests for duplicate imports, missing roots, and state restore**

Cover:
- repeated folder import does not duplicate tracks
- missing/deleted roots degrade gracefully
- selected playlist/current track restore remains valid after refresh

- [ ] **Step 2: Run focused repository tests**

Run: `.\gradlew.bat :desktop-app:test --tests "com.yaneodex.desktop.integration.DesktopLibraryRepositoryTest" --console=plain`

- [ ] **Step 3: Implement repository/persistence hardening**

Requirements:
- deterministic root normalization
- graceful cleanup of dead paths
- sane restore after content changes
- predictable favorite/default playlist behavior

- [ ] **Step 4: Verify repository tests pass**

Run: `.\gradlew.bat :desktop-app:test --tests "com.yaneodex.desktop.integration.DesktopLibraryRepositoryTest" --console=plain`

- [ ] **Step 5: Commit**

```bash
git add desktop-app/src/jvmMain/kotlin/com/yaneodex/desktop/integration/DesktopLibraryRepository.kt desktop-app/src/jvmMain/kotlin/com/yaneodex/desktop/integration/DesktopPersistence.kt desktop-app/src/jvmMain/kotlin/com/yaneodex/desktop/app/DesktopController.kt desktop-app/src/test/kotlin/com/yaneodex/desktop/integration/DesktopLibraryRepositoryTest.kt
git commit -m "fix: harden desktop library management"
```

### Task 6: Add first-run and settings polish required for a marketable desktop app

**Files:**
- Modify: `C:\Users\vladg\music-offline-desktop\desktop-app\src\jvmMain\kotlin\com\yaneodex\desktop\ui\MusicDesktopApp.kt`
- Modify: `C:\Users\vladg\music-offline-desktop\desktop-app\src\jvmMain\kotlin\com\yaneodex\desktop\ui\DesktopStrings.kt`
- Modify: `C:\Users\vladg\music-offline-desktop\desktop-app\src\jvmMain\kotlin\com\yaneodex\desktop\app\DesktopController.kt`

- [ ] **Step 1: Define the exact first-run path**

Include:
- add folders
- refresh scan
- parser available but optional
- OCR optional and clearly secondary

- [ ] **Step 2: Implement settings/onboarding cleanup**

Requirements:
- no dev-style labels or clutter
- clear empty states
- obvious primary action on first launch
- resilient messages after permission/network failures

- [ ] **Step 3: Run manual smoke**

Checklist:
- clean state launch
- import a folder
- play a track
- parser preview/download/add
- OCR screen with and without configured backend

- [ ] **Step 4: Commit**

```bash
git add desktop-app/src/jvmMain/kotlin/com/yaneodex/desktop/ui/MusicDesktopApp.kt desktop-app/src/jvmMain/kotlin/com/yaneodex/desktop/ui/DesktopStrings.kt desktop-app/src/jvmMain/kotlin/com/yaneodex/desktop/app/DesktopController.kt
git commit -m "feat: complete desktop onboarding and settings polish"
```

---

## Chunk 4: Release and Marketable Delivery

### Task 7: Make updates, packaging, and smoke repeatable

**Files:**
- Modify: `C:\Users\vladg\music-offline-desktop\desktop-app\build.gradle.kts`
- Modify: `C:\Users\vladg\music-offline-desktop\scripts\package.ps1`
- Modify: `C:\Users\vladg\music-offline-desktop\scripts\smoke.ps1`
- Modify: `C:\Users\vladg\music-offline-desktop\README.md`

- [ ] **Step 1: Write the release checklist**

Include:
- clean package commands
- expected output paths
- version bump rule
- MSI update/install test

- [ ] **Step 2: Implement packaging defaults for MMP**

Requirements:
- one canonical MSI path
- one canonical EXE path
- no accidental release path that breaks JavaFX
- versioned updates actually replace older installs

- [ ] **Step 3: Expand smoke script**

Smoke should cover:
- compile
- tests
- distributable creation
- installer creation
- startup validation of built app

- [ ] **Step 4: Verify full release path**

Run:
- `.\scripts\test.ps1`
- `.\scripts\build.ps1`
- `.\scripts\package.ps1`
- `.\scripts\smoke.ps1`

- [ ] **Step 5: Commit**

```bash
git add desktop-app/build.gradle.kts scripts/package.ps1 scripts/smoke.ps1 README.md
git commit -m "chore: finalize desktop mmp release workflow"
```

### Task 8: Final acceptance pass for MMP

**Files:**
- Modify: `C:\Users\vladg\music-offline-desktop\README.md`

- [ ] **Step 1: Run full user acceptance checklist**

Acceptance:
- install from MSI
- launch from `Program Files`
- import local library
- play/pause/next/previous/shuffle
- parser search/preview/download/add
- OCR screen opens and handles missing/valid config
- window resize does not break layout

- [ ] **Step 2: Capture known limitations**

Document remaining non-blockers:
- codec limitations from Windows media stack
- OCR backend dependency
- parser source availability variance

- [ ] **Step 3: Update README from MVP to MMP**

Shift wording from “MVP” to “Windows desktop player with parser and OCR support”.

- [ ] **Step 4: Commit**

```bash
git add README.md
git commit -m "docs: mark desktop app as mmp-ready"
```

---

## Final Verification

- [ ] Run: `.\gradlew.bat :desktop-app:compileKotlin --console=plain`
- [ ] Run: `.\gradlew.bat :desktop-app:test --console=plain`
- [ ] Run: `.\scripts\build.ps1`
- [ ] Run: `.\scripts\package.ps1`
- [ ] Run: `.\scripts\smoke.ps1`

Plan complete and saved to `C:\Users\vladg\music-offline-desktop\docs\superpowers\plans\2026-03-26-mmp-local-player.md`. Ready to execute?
