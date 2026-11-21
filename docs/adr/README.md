# Architecture decision records

One file per decision: what was decided, why, and what it costs. ADRs 0001-0003 were
written on 2026-11-20 from the code, its comments and the message of commit `710a7ee`,
after the decisions were made; they record the reasons that are visible in the repo and
say so where a number has no recorded derivation.

| ADR | Decision | Status |
|-----|----------|--------|
| [0001](0001-pipeline-on-decision-model.md) | Build the gate, judge and re-draft loop in this repo on `DecisionModel` | Accepted |
| [0002](0002-confidence-gate-thresholds.md) | Confidence gate: floor 0.60, `auto_close` needs 0.90 | Accepted |
| [0003](0003-reject-after-three-drafts.md) | Judge minimums, 3 drafts, then throw and queue for a human | Accepted |
| [0004](0004-released-dependencies-only.md) | Released dependencies only; no snapshot BOM | Accepted |
| [0005](0005-startup-demo-opt-in.md) | The startup demo is opt-in | Accepted |

New ADR: copy the shape of an existing one (Context, Decision, Consequences), take the
next number, add a row here.
