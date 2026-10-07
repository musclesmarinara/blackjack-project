# Blackjack Observer v0.9 — repair test build

This Android app observes screen cards and displays a provisional Hi-Lo count
and advice for one selected seat. This is an experimental test build. It has
NOT passed automatic-recognition acceptance; read VALIDATION.md.

## Install

Install BlackjackObserver-test-v0.9.apk as an update. Same package and development
signing key as v0.8; version code 9, minimum Android 8/API 26.

## Start

1. Start observation and allow the overlay and full-screen capture.
2. Open the table. Keep orientation and browser zoom fixed. Place the overlay
   outside the physical-card area.
3. Wait until all cards and hands are clear of that area. Open Calibrate, which
   freezes the screen. Use one table rectangle covering physical dealer/player
   cards, excluding chips, shoes, digital duplicates and neighboring tables.
4. Put the dealer and seven player anchors at first-card positions. Seats stay
   numbered 1–7 left to right, including empty seats. Angle controls describe
   clockwise card tilt. Tap Learn empty, Save calibration, then Start.
5. Green RUNNING means scanning is enabled; red STOPPED means scanning is off.
   Seat selects advice only. All recognized seats contribute to the count.
6. Learn a fresh empty reference at every capture session or layout/camera change.
   Use Test frame to inspect reads; the preview itself never changes the count.
7. Record a deal-to-clear cycle with the built-in recorder. Export its MP4 and
   diagnostics from the main app. Running another screen recorder can stop capture.

## Repairs in this build

- Confirmed cards count before a complete round or readable dealer is available.
- Starting a round retains previously accepted cards instead of recounting them.
- Empty-table and shoe transitions discard stale visual identities before they
  can be added back into a reset ledger.
- Manual correction retains prior recognition watermarks. Manual intervention
  still marks the shoe uncertain; this does not solve every occlusion/correction case.
- New-shoe confirmation exits burn mode and cancels a pending duplicate reset.
- Background comparison preserves connected white card faces over table markings;
  morphology is applied with references and clothing above the dealer is excluded.
- Foreshortening correction precedes deskewing; card tilt is subtracted.
- The reader chooses one normal/inverted printed-corner orientation per seat and
  holds that choice until a confirmed boundary. It does not deliberately feed
  both orientations into tracking. This is not a physical-card identity model.
- Logs contain actual hand angles, OCR confidence and chosen corner orientation.
- Capture retains native display resolution instead of capping width at 1080px.

## Remaining limitations

The last completed Android image replay still missed visible cards. The final
corner-selection change has unit coverage but no new end-to-end accuracy result.
Do not assume that a moving count is correct. Angled/overlapping/hidden cards,
symmetric ranks, camera motion and blur can still cause omissions or duplicates.
Manual corrections, split reconstruction, visual shoe/cut-card recognition and
exact automatic burn tracking are not fully solved. Hidden ranks are never guessed.
Missing cards make the shoe count incomplete. The app uses limited strategy indices,
not an exact composition-dependent optimal strategy solver.

## Build

Use JDK 17, Android SDK 35 and Gradle 8.9 (wrapper included):

    ./gradlew assembleDebug lintDebug
    bash tests/run.sh

APK: app/build/outputs/apk/debug/app-debug.apk

All application code, resources, build configuration, tests, Gradle wrapper and
development signing files are included. Gradle downloads third-party dependencies;
Android SDK/JDK and ML Kit source code are not bundled. The public development
keystore preserves update compatibility and is unsuitable for production signing.

The companion CompleteSource.txt is valid Python 3 containing the complete project
as readable source strings and base64-encoded binary support files. To recreate
it, save the entire file as restore.py and run python3 restore.py in an empty
folder. It refuses to overwrite existing files and verifies every file hash.
Do not paste the bundle into a single Java file.
