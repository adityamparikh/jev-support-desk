# ADR 0003: Judge minimums, 3 drafts, then throw and queue for a human

- Status: Accepted (commit `710a7ee`; recorded 2026-11-20)

## Context

The chat model's draft goes to a customer, so it has to pass a review first. The review
is `ReplyJudge`: four Jev questions, each with a minimum, plus local checks.

## Decision

- Minimums (in `ReplyJudge`): `addresses_issue` probability >= 0.7,
  `no_unapproved_promise` >= 0.8, `no_leak` >= 0.8, `tone` level >= 1.5 on a 0..2 scale
  (so only the top level, "Empathetic and professional", passes). The input screen
  refuses a ticket when `injection` probability >= 0.5. No reason for the individual
  values is recorded in the repo; note that the two checks guarding against something
  that cannot be taken back once sent (an unapproved promise, a leak) carry the higher
  bar.
- `ReplyDrafter` feeds the judge's defects back into the next prompt and stops after
  `MAX_ATTEMPTS = 3`. It then throws `ReplyRejectedException` instead of returning its
  best attempt (its Javadoc: "throws instead of returning the best effort"). The effect
  is that every draft it returns has passed the judge, so no caller has to check.
- `TicketController` maps the exception to `202 Accepted` with
  "Queued for a human: <defects>": the request was taken, a person finishes it.

## Consequences

- Worst case per ticket: 1 input screen + 3 drafts + 3 judge calls, which bounds spend.
- The minimums are not derived from a calibration run recorded in this repo; changing
  one means changing the matching just-below-the-minimum case in `ReplyJudgeTest`.
