# Blackjack Project v.01

Based on the supplied BlackjackOverlay-v0.9-FINAL-source.txt.
App label: Blackjack Project v.01. Version name: v.01. Android version code: 10.
Package and included development signing key retained for update compatibility.

## Review changes

- Removed automatic background learning and automatic scanner start. No detected
  card groups did not prove that the table was empty: ambiguous, rejected, small,
  or obscured cards could all produce zero groups. Learning such a frame could
  suppress real cards from later recognition. Calibration UI could also trigger it.
- Background learning is now explicitly requested through Learn empty. Scan start
  checks the reference on the recognition worker and ignores requests invalidated
  by Stop, calibration, or shutdown.
- Stopped overlay displays the calibration diagnostic instead of hiding it.
- Retained the supplied removal of the OCR confidence gate; no threshold restored.
- Updated the launcher, main screen, overlay, notification and diagnostics branding.
- Build workflow now requires passing regression tests; output is named
  Blackjack-Project-v.01.apk.

## Current validation

All nine existing pure-Java regression suites passed on October 6, 2026.
Android assembly/lint could not run: Gradle 8.9 download failed with Network is
unreachable, and no Android SDK is installed in this execution environment.
No v.01 APK was produced. Android UI changes are not compilation-verified here.
Recognition accuracy on real video or a phone has NOT been validated.
Historical validation documents describe earlier versions only.

## Build the APK

With JDK 17, Android SDK 35 and network access:

    bash tests/run.sh
    bash gradlew assembleDebug lintDebug

Result: app/build/outputs/apk/debug/app-debug.apk.
Rename that file Blackjack-Project-v.01.apk.
Alternatively upload this project to your own GitHub repository, including the
.github directory, and run the included Build Blackjack Project v.01 workflow.
Download the Blackjack-Project-v.01 artifact from its successful run and extract
its APK. The workflow has been supplied but was not run in this session.

The included development signing key is public test material, not a production key.

## Use

1. Start observation and allow overlay and full-display capture.
2. Open the table and place the overlay outside the card area.
3. Open Calibrate while the physical card area is empty and hands are clear.
4. Configure one table region and the dealer/seven player anchors and angles.
5. Tap Learn empty, Save calibration, then Start.
6. Confirm the overlay says RUNNING; inspect its region/OCR/stable-card diagnostics.
7. Use the built-in recorder and export logs for actual device validation.

Repeat empty-reference calibration when capture restarts or the table layout changes.
This remains an experimental observer. It does not place bets, and missed/overlapping
cards can make counts incomplete. A moving count is not proof of an accurate count.
