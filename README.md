# Workly

**English** | [中文](README.zh-CN.md)

**Website:** <https://byxcxc.github.io/workly/> · **Source:** <https://github.com/byxcxc/workly>

[![Android CI](https://github.com/byxcxc/workly/actions/workflows/android.yml/badge.svg)](https://github.com/byxcxc/workly/actions/workflows/android.yml)

**Personal work hours and earnings tracker.**

Workly is a quiet, offline Android app for people who are paid by the hour.
Open it, tap **Start work**, get on with your day, tap **Finish work** — Workly
works out how long you worked and how much you earned.

There is no account, no cloud, no ads and no analytics. Everything lives in a
local database on your device and the core app works completely offline.

> 日本語: Workly は、時間給で働く人のための静かなオフライン記録アプリです。
> 「作業を開始」を押して、終わったら「作業を終了」を押すだけ。アカウントも
> クラウドも広告もなく、データは端末内のデータベースにのみ保存されます。

---

## Features

**Tracking**

- One-tap **Start work** / **Finish work** with a live timer and live earnings
- The running session is written to the database immediately, so closing the app,
  locking the screen, a process kill or a phone reboot never loses it
- The elapsed time is always calculated as `now - startTime`, so it stays correct
  even while the app is not running
- **Finish** opens an editable summary: end time, break, hourly rate, work type
  and note — all adjustable before saving
- Manual **Add record** for work you forgot to track
- Edit and delete any record, with confirmation before deleting

**Insight**

- **Home** dashboard: worked today, earned today, this week, recent records
- **Records**: chronological list *and* a month calendar with per-day totals
- **Statistics**: this week / this month / this year / custom range, with total
  time, total income, work days, averages and two simple trend charts

**Insight, charts that say something**

- Every chart states its numbers: values are drawn on the bars, and every single
  data point is also listed underneath in a row you can scroll sideways
- Work schedule: a weekly hours target and the days you do not work. The
  dashboard and statistics show progress against the target, scaled to the
  period you are looking at, and the calendar marks your rest days
- The **custom range** in Statistics takes its own total-hours figure and updates
  the projected income as you type
- Long-press any day in the calendar to **record work for that day** straight
  away, to force it to be a rest day, or to give it its own hours target. Every
  choice is remembered

**Make it yours**

- **Background**: the theme default, a solid colour, or your own picture picked
  from the gallery
- **Gaussian blur** on the picture, adjustable from 0% to 100%
- **Glass surfaces**: with a custom background every card becomes slightly
  translucent, so the blurred picture shows through
- **Match the picture**: the buttons and charts take their colour from the
  picture's dominant colour

> **Note:** the Gaussian blur uses the platform's render effect, which Android
> exposes from Android 12 (API 31). On older devices the picture is still shown,
> just without the blur, and everything else works the same.


**Configuration**

- Default hourly rate and currency (per currency decimal rules — ¥12,480 or
  $12,480.50)
- Work types with their own default rate; choosing a type fills in its rate
- Light / dark / system theme plus six theme colours (indigo, teal, forest,
  sunset, rose, graphite)
- Durations as `6h 30m` or as `6.5h`, whichever reads better to you
- First day of the week, weekly target, rest days
- **Export** to CSV (spreadsheet friendly) or JSON (complete backup) and
  **import** a JSON backup with duplicate detection
- **Log time manually** straight from the dashboard, for work you forgot to track

**Quality**

- English, Japanese and Simplified Chinese, all text in resources
- In-app language picker (and the system "App language" screen on Android 13+),
  with the choice remembered across launches
- Material 3, dark theme, generous touch targets, content descriptions,
  screen-reader friendly values and support for large font scaling
- Money is stored as integer minor units — no floating point rounding drift
- Compose state holders are annotated `@Immutable` and the once-a-second timer
  is scoped to the two numbers it updates, so lists and cards skip recomposition
- Times are stored as `Instant` (UTC) and only converted to the device time zone
  for display, so travel and time-zone changes never shift history
- Cross-midnight sessions (`23:00 → 02:00 = 3h`) are handled correctly
- Recorded sessions keep a snapshot of the rate and work type name they were
  saved with, so editing a default rate never rewrites past earnings

---

## Screenshots

Screenshots are rendered from the `@Preview` composables in
`app/src/main/java/com/workly/app/ui/preview/DesignPreviews.kt`. Open that file
in Android Studio and use the preview pane to render and export them into
`docs/screenshots/`.

| Screen | What it shows |
| --- | --- |
| Home | Worked today, earned today, this week, recent records |
| Home (working) | Live timer, live earnings, Finish work |
| Records | List view grouped by day |
| Calendar | Month grid, work markers, selected-day totals |
| Statistics | Totals, averages and trend charts |
| Settings | Rate, currency, work types, theme, background, backup |

---

## Tech stack

| Layer | Choice |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| State | ViewModel + `StateFlow`, unidirectional data flow |
| Persistence | Room (sessions, work types) + DataStore (settings) |
| Async | Kotlin Coroutines |
| Serialization | kotlinx.serialization (JSON backup) |
| Build | Android Gradle Plugin 9.4 with **built-in Kotlin**, Gradle 9.7, KSP |
| SDK | `minSdk 26`, `targetSdk 37`, `compileSdk 37` (Android 17) |
| Tests | JUnit4 (JVM) + Compose UI test / Espresso (instrumented) |

The project deliberately avoids a DI framework and a full Clean Architecture
split: Workly is a single-module app and stays readable for someone who is still
learning Android.

---

## Architecture

```
UI (Compose screens)
      ↓  events
ViewModel (StateFlow<UiState>)
      ↓
Repository (WorkRepository, WorkTypeRepository, SettingsRepository)
      ↓
Room database (work_sessions, work_types)  +  DataStore (settings)
```

- **Unidirectional data flow.** Screens render an immutable `UiState` and send
  events back to the ViewModel. They never touch the database directly.
- **Pure domain layer.** `domain/` contains the time, money, validation,
  statistics and calendar logic. It has no Android dependencies and is fully unit
  tested — including `23:00 → 02:00` and `8h − 1h = 7h`.
- **`Result` instead of exceptions.** Repositories return
  `Result<T>`; failures carry a `WorklyError` that the UI maps to a localized
  message. A stack trace is never shown to the user.
- **Two deliberate deviations from the original data model**, both noted here for
  transparency:
  1. Settings live in **DataStore**, not in an `AppSettings` Room table. They are
     a handful of primitives, and DataStore gives observable, atomic updates with
     no schema or DAO boilerplate.
  2. Room entities are used directly as models instead of adding a parallel
     `domain/model` copy plus mappers. Computed values (`workedMinutes`,
     `incomeMinor`) are extension properties that delegate to the pure domain
     functions, so there is still one place where the rules live.

### Project layout

```
app/src/main/java/com/workly/app/
├── AppGraph.kt               # tiny hand-written dependency container
├── MainActivity.kt           # single activity, edge-to-edge Compose
├── WorklyApplication.kt
├── data/
│   ├── backup/               # CSV + JSON export, JSON import, dedupe
│   ├── local/                # Room database, DAOs, entities, converters
│   ├── prefs/                # DataStore settings
│   └── repository/           # WorkRepository, WorkTypeRepository
├── domain/                   # Money, WorkTime, SessionValidator, stats, ranges
└── ui/
    ├── background/           # picture decoding, accent extraction, backdrop
    ├── components/           # cards, rows, empty state, dialogs, icons
    ├── home/                 # dashboard + live timer
    ├── navigation/           # routes and NavHost
    ├── preview/              # @Preview design gallery
    ├── records/              # list, calendar, detail, add/edit/finish editor
    ├── settings/             # settings, work types, about
    ├── statistics/           # period stats + trend charts
    ├── theme/                # colours, type scale, shapes, accents
    └── util/                 # formatting and error messages
```

`app/src/main/res/values/` holds the English strings, `values-ja/` the Japanese
ones and `values-zh/` the Simplified Chinese ones. Nothing user-visible is
hard-coded in Kotlin.

---

## Build

### Requirements

- Android Studio (current stable) **or** a command-line toolchain
- JDK 17 or newer (JDK 21 recommended)
- Android SDK with **platform 37.0** and **build-tools 36.0.0**

### From Android Studio

1. `File → Open…` and select the project root.
2. Let Gradle sync (it downloads the Gradle wrapper and the dependencies).
3. Press **Run ▶** to install on a device or emulator.

### From the command line

```bash
git clone https://github.com/<your-user>/workly.git
cd workly

# Point Gradle at your SDK if it is not in ANDROID_HOME
echo "sdk.dir=$HOME/Android/Sdk" > local.properties

./gradlew assembleDebug          # build the debug APK
./gradlew installDebug           # install it on a connected device
./gradlew assembleRelease        # unsigned release build (R8 enabled)
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

### Release signing

No keystore is committed to this repository, and the release build stays unsigned
until you provide one. To sign locally, create `keystore.properties` in the
project root (it is git-ignored):

```properties
storeFile=/absolute/path/to/release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

For CI, add `WORKLY_KEYSTORE_BASE64`, `WORKLY_KEYSTORE_PASSWORD`,
`WORKLY_KEY_ALIAS` and `WORKLY_KEY_PASSWORD` as repository secrets and follow the
commented template at the bottom of `.github/workflows/android.yml`.

---

## Testing

```bash
./gradlew testDebugUnitTest        # JVM unit tests (no device needed)
./gradlew connectedDebugAndroidTest # instrumented tests (device or emulator)
```

The JVM suite is 90 tests and the instrumented suite is 40 tests, and both are
green in CI.

**JVM unit tests** (`app/src/test/`) cover the rules that are easy to get wrong:

- time arithmetic, including `23:00 → 02:00 = 3h` and a full 24 hour shift
- `8h work − 1h break = 7h`
- income maths and half-up rounding, JPY (0 decimals) vs USD (2 decimals)
- validation: identical start/end, break longer than the work, negative rate
- daily / weekly / monthly / yearly aggregation, work days and averages
- the work schedule: planned work days, rest days, period targets and progress
- per-day overrides: forcing a rest day, forcing a working day, a day's own hours
- decimal-hour formatting (`6.5`, `8`, `7.33`)
- week ranges for a configurable first day of the week
- month calendar grid shape and padding
- CSV escaping, ordering and money formatting
- JSON backup round-trip, unknown fields, malformed input, version refusal

**Instrumented tests** (`app/src/androidTest/`) run against a real Room database
and the real UI on an emulator:

- DAO and schema behaviour (converters, unique indexes, foreign key `SET_NULL`)
- repository rules: start, finish, discard, edit, delete, import de-duplication
- CSV/JSON export and JSON import round trips
- Compose flows: start work, finish work, add, edit, delete, manual entry from
  the dashboard, the language picker, long-pressing a calendar day, bottom
  navigation, and restoring a running session after leaving the dashboard

The instrumented tests need a device because Room requires the Android SQLite
implementation.

---

## GitHub Actions

| Workflow | Trigger | What it does |
| --- | --- | --- |
| `Android CI` (`.github/workflows/android.yml`) | push / PR to `main`, manual | JDK 21 + Android SDK, `testDebugUnitTest`, `lintDebug`, `assembleDebug`, uploads the debug APK and the reports as artifacts |
| `Instrumented tests` (`.github/workflows/android-instrumented-tests.yml`) | manual, weekly | boots an emulator and runs `connectedDebugAndroidTest` |

No secrets are required for either workflow, and nothing sensitive is stored in
the repository.

---

## Roadmap

- [ ] Home screen widget and a quick-settings tile for start/stop
- [ ] Optional reminder notification if a session runs unusually long
- [ ] Overtime / multiplier rates and per-day rate overrides
- [ ] A dashboard widget showing progress towards the weekly target
- [ ] Tags and a search field in Records
- [ ] Additional languages (the resource layout is ready)
- [ ] Optional encrypted cloud sync — still no account required for the core app

---

## Website

The project page in `docs/` is published with GitHub Pages:

<https://byxcxc.github.io/workly/>

It is a single static page (no build step): `docs/index.html`, `docs/assets/style.css`
and `docs/assets/app.js`. Copy lives in a small dictionary in `app.js` so the page
switches between English, 中文 and 日本語 the same way the app does.

To preview it locally:

```bash
python3 -m http.server 8000 --directory docs
# then open http://localhost:8000
```

## Repository

<https://github.com/byxcxc/workly>

## License

[MIT](LICENSE) © 2026 Workly contributors
