# v0.6 validation

Regression tests: confirmed cards can count despite an unrelated group warning; the prior negative-group gate reproduces the round-zero failure; empty tables request identity reset before any detected round; no cards or rounds are invented by that reset.

Replay of all 344 sample events from Blackjack-diagnostics.jsonl (2), using recorded visible ranks, group counts and timestamps, produces one round start instead of zero and 24 empty-table confirmations. These repeated confirmations occur during long empty stretches; they are not a count of completed rounds. The replay does not rerun image recognition or validate the physical-card count.

Existing tests pass: 31 core checks, 238 layout assertions, 25 scanner/filter checks, 13 grouping/round checks and 7 identity checks.

Android build/signature status and APK hash are appended during release packaging. No Android device or emulator runtime test was performed. Missing card reads in the phone diagnostics remain a recognition limitation; this release addresses the global round-start block and stale identity reset.

Release: Android assembleDebug and lintDebug passed; zero lint errors. APK v2 signature verified; versionCode 6 / versionName 0.6-round-fix verified.

APK SHA256: 3e518c36ad4cbe3e015b15c807e3d620a9ebd48a9cc8019f0fea973ff3c958f8
