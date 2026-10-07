# v0.9 validation status — October 3, 2026

Automatic card-recognition acceptance is NOT passed. This package completes the
code/build delivery, not the user's goal of a proven accurate automatic counter.

## Evidence from the completed September 29 investigation

Blackdiag08.jsonl contained 225 samples, zero observed cards and zero recognized
rounds. Only three samples had a stable rank, at most one; none had a stable dealer
rank. Confirmed cards were gated behind full-round recognition in v0.8.

The actual bundled ML Kit engine was tested in an Android API 30 software emulator
on six samples from blackjacv08n.mp4. Input was 720x1560 after removing its 180px
recorder footer. The 45-second sample was the empty reference, and the last logged
table/anchor coordinates were used. Actual historical hand angles were unavailable
because v0.8 logged the legacy angle array rather than the one used by its reader.

The first replay exposed an incorrect deskew direction. The second replay after
that fix still failed. At the 15-second extraction slot it recovered a 4 while
missing the 10 in the same hand and the other seat's ranks. At the 80-second slot,
readable A and K tokens were rejected as inverted. This motivated the final
consistent-corner selector. That selector is implemented and unit-tested in the
current build, but a new full Android image replay has not validated it.

Each replay image was processed once with a reset tracker. Its stable=0 result
was therefore expected and must not be quoted as an end-to-end count metric.
These were rank-candidate tests, not a complete temporal video or shoe benchmark.
The prior raw scratch outputs were removed by workspace maintenance; the source
and documented changes were recovered for this delivery. No new accuracy figures
are invented from those unavailable outputs.

## Current checks

The pure-Java suite covers core counting, layout, scan start/stop, grouping,
physical-identity safeguards, table components, temporal tracking, round-state
regressions and the new corner selector. It tests early confirmed counting,
no recount at text/visual round starts, stale manual-correction reads, burn/shoe
transitions, white-face continuity, clothing exclusion, one orientation at a time,
and orientation retention until a boundary. Results: docs/core-results.txt.

Build/signature results for the delivered binary are recorded in BUILD-RESULTS.txt.
Compilation, lint and signing are not recognition-accuracy tests. Samsung capture,
overlay runtime, native-resolution performance, long-run count accuracy and thermal
behavior are not newly validated in this delivery.

## Remaining engineering work

A validated physical-card/corner detector and rank classifier is still needed,
with labelled native-resolution images covering every rank, all seats, overlaps,
repeated cards, hits, split hands and clean boundaries. Corner orientation alone
cannot prove that two recognized glyphs represent two different physical cards.
The movement safeguard can also withhold new hits when an earlier corner disappears.
Manual correction watermarks can withhold later cards in ambiguous combinations;
manual intervention remains explicitly marked as uncertain.

Cut-card classification, exact burns and automatic visual shoe replacement are
not solved. Preserve the manual review controls and do not infer hidden ranks.
