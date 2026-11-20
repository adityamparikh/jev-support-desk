# ADR 0001: Build the gate, judge and re-draft loop in this repo on `DecisionModel`

- Status: Accepted (commit `710a7ee`, 2026-11-06; recorded 2026-11-20)

## Context

The first version (`34ca8b0`) used `spring-ai-typesafe`, which shipped a Jev judge,
advisors and `JevConfidenceGate`. The Spring AI 2.1 System One module
(`spring-ai-systemone`) replaces it with a generic `DecisionModel` that answers typed
questions (`YesNoQuestion`, `ChoiceQuestion`, `ScoreQuestion`) and nothing more: no
judge, advisor or gate classes, and, used without its starter, no auto-configuration.

## Decision

- `ConfidenceGate`, `ReplyJudge` and `ReplyDrafter` are plain classes in
  `com.example.supportdesk`, built on `DecisionModel`.
- `JevConfig` declares the `SystemOneClient` and `DecisionModel` beans by hand.
- Exact rules are code, not Jev questions. The empty-reply check in `ReplyJudge.judge`
  runs locally and returns before any Jev call. `ReplyJudge`'s Javadoc gives the reason:
  exact rules "are free, exact, and never sent to Jev".
- Two independent answers are combined in code into one policy decision
  (`TriageService`: `auto_close` only if urgency < 0.2 and frustration < 0.5), rather
  than asking Jev a compound question, so each answer stays separately thresholdable.

## Consequences

- The thresholds and the retry policy are visible and unit-tested here
  (`ConfidenceGateTest`, `ReplyJudgeTest`, `ReplyDrafterTest`) instead of hidden in a
  library.
- Library upgrades can still move the ground: the 2026-11-15 snapshot replaced the
  client class under `JevConfig` (see ADR 0004).
