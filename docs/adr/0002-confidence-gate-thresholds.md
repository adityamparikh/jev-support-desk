# ADR 0002: Confidence gate, floor 0.60, `auto_close` needs 0.90

- Status: Accepted (commit `710a7ee`; recorded 2026-11-20)

## Context

Triage produces an action (`auto_close` or `route_to_<department>`) and a confidence,
the department answer's reported confidence (falling back to its probability). Acting on
a wrong answer costs different amounts depending on the action.

## Decision

`TriageService` builds `ConfidenceGate.builder().floor(0.60d).require("auto_close", 0.90d)`:

- confidence < 0.60: `ESCALATE` (no draft; a human writes the reply);
- at or above the action's requirement: `EXECUTE`;
- in between: `CONFIRM` (a draft is produced, a human confirms the action).

Actions without their own requirement use the floor, so every `route_to_*` executes at
0.60. The reason is the comment beside the gate: "Routing is cheap to undo; closing a
ticket unread is not." A misrouted ticket is re-routed by the next person who reads it;
an auto-closed one may never be read.

## Consequences

- `auto_close` between 0.60 and 0.90 always needs a confirmation.
- The exact values 0.60 and 0.90 are not derived from a calibration run recorded in this
  repo. Changing them changes the numbers the article quotes, so update the boundary
  rows in `ConfidenceGateTest` and `TriageServiceTest` in the same change.
