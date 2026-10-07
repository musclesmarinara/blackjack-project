# v0.7 validation — 2026-09-23

## Scope of this release

The Android pipeline now scans one physical-table ROI rather than seven overlapping
seat ROIs and a duplicate digital dealer ROI. Movable point anchors assign dealer
and seat identities. Component crops are deskewed, OCR candidates are tracked
in global coordinates, and selected-seat/group advice is separate from shoe counting.
Existing green/red Start/Stop controls remain.

## Automated checks

Run `sh tests/run.sh` with JDK 17. Completed successfully:

- 31 core count/strategy checks.
- 238 legacy card-layout assertions.
- 25 scanner lifecycle/orientation-filter checks.
- 13 legacy grouping/round checks.
- 7 legacy identity checks.
- Round-start and pre-round identity-clear regressions.
- 13 new global table-tracker checks: identical ranks, empty-seat numbering,
  repeated observations, new hits, separated groups, occlusion, large movement,
  ownership conflicts, duplicate observations, rank conflicts, reset boundaries,
  consecutive confirmation, and advice gating during normal confirmation delay.
- 6 new synthetic table-component checks: physical dealer plus fixed seat
  ownership, exclusion outside the table ROI, removal of thin lettering,
  merged-seat rejection, ambiguous nearest-anchor rejection, and unresolved-face protection against false clears.

These tests validate bookkeeping and synthetic geometry. They do not validate
ML Kit recognition of actual playing-card ranks or the correctness of a shoe count.

## Android build

`assembleDebug lintDebug` completed successfully with Gradle 8.9, JDK 17 and
Android SDK 35. The final incremental build recompiled the changed sources.
Lint reported 0 errors and 15 warnings (existing accessibility, text/localization,
RTL, view-constructor and data-extraction-rule issues).

The included Gradle wrapper was generated successfully and pins the distribution
SHA-256. Its initial network bootstrap was blocked by this environment's proxy;
the build used the downloaded Gradle 8.9 distribution directly. No environment
proxy settings or SDK paths are included in the project.

## Unverified behavior and remaining risks

No Samsung install, live table, or on-device ML Kit video-replay accuracy test has
been performed on v0.7. A debug build compiling is not evidence of detection accuracy.
The previously evaluated off-the-shelf YOLO models are not used in this release.
No custom model was trained.

The detector still uses light-face components and OCR, not a physical-card instance
segmentation model. A confirmed rank may still be wrong; opposite corners can
still be misinterpreted; unreadable cards can be missed without a warning.
Nearest-anchor grouping may misassign cards when dealing spreads into a neighbour's
space. Large movements are held for review rather than guessed as new cards.

Separated groups at one seat are selectable but automatic split reconstruction is
not reliable. The displayed hand must be checked, especially when groups merge or
disappear. A touching split pair can be interpreted as one hand. Dealer advice needs
the upcard to have been observed alone; joining after its reveal may leave WAIT.

Old calibration is invalidated; anchors and table crop must be checked before
Start. Test frame shows candidates without three-frame confirmation. It never
changes the running count. Recording is still downscaled by the legacy recorder;
that limits remote evaluation of tiny ranks.

Cut-card appearance is not an automatic reset signal. The new table pipeline does
not call the former barcode-texture heuristic. Shoe/burn confirmation is manual or
uses the existing game-status text logic; hidden burned ranks cannot be recovered.
The old fixed-position seat-total OCR cross-check is not used by the table pipeline;
there is no independent total check in this release.
The strategy implementation remains the existing basic strategy and limited count
indices, with its existing rule assumptions.

## Suggested phone acceptance test

After building, install the debug APK, recalibrate, and observe without wagering.
Record a complete deal-to-clear sequence, including an empty seat and at least one
hit. Compare every physical exposed card with the `tracks`, `confirmed`, `hands`
and count entries in exported diagnostics. Verify the dealer's digital duplicate
is excluded, repeated frames do not add cards, seat changes do not change RC, Stop
halts updates, and separated hands are not concatenated in the displayed advice.
Any missed/extra card fails counting validation and requires further detector work.
