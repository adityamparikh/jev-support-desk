# Spec 0001: Triage and draft pipeline

- Status: Shipped (`710a7ee`, 2026-11-06). Written on 2026-11-20 from the code and tests
  to record the behaviour that commit shipped; nothing here is new.
- Related: ADR 0001, 0002, 0003

## Goal

`POST /tickets` with a `text/plain` ticket returns a triage (action, department,
confidence, gate decision) and, when the gate allows it, a reply draft that a Jev judge
has approved. The same pipeline runs once at startup when the demo is enabled.

## Non-goals

- No persistence: nothing is stored; every request is independent.
- No sending: the app drafts replies, it never delivers them to a customer.
- No authentication on `/tickets`: this is a local demo, not a deployable service.
- No tuning of thresholds against real data (see ADR 0002 and 0003).

## Acceptance criteria

- [x] AC1 One Jev call asks `is_urgent`, `department` and `frustration` together, in that
  order (System One sends questions in map order, which can change an answer).
  `TicketQuestionsTest#triageMapContainsExpectedQuestions`.
- [x] AC2 Action is `auto_close` iff urgency < 0.2 and normalised frustration < 0.5, else
  `route_to_<department>`. `TriageServiceTest#boundaryConditionsForActionDecision`
  (`0.19, 0.49 -> auto_close`; `0.20, 0.50 -> route_to_sales`).
- [x] AC3 Gate: confidence 0.59 -> `ESCALATE`, 0.60 -> `EXECUTE` for `route_to_*`;
  `auto_close` at 0.89 -> `CONFIRM`, at 0.90 -> `EXECUTE`.
  `ConfidenceGateTest#decidesFromFloorAndPerActionRequirement`.
- [x] AC4 `ESCALATE` returns `"draft": null` and never calls the chat model.
  `TicketControllerTest#submitTicketWithEscalateDecisionReturnsNullDraftWithoutCallingChatClient`.
- [x] AC5 `EXECUTE` and `CONFIRM` return the judge-approved draft.
  `TicketControllerTest#submitTicketWithExecuteDecisionReturnsDraft`,
  `#submitTicketWithConfirmDecisionReturnsDraft`.
- [x] AC6 A failed review re-drafts with the defects in the prompt.
  `ReplyDrafterTest#redraftsWithDefectsFedBackUntilJudgePasses`.

## Failure cases

- [x] Judge fails 3 drafts: `ReplyRejectedException`, never the best attempt
  (`ReplyDrafterTest#throwsAfterMaxAttemptsInsteadOfReturningBestEffort`), and the HTTP
  response is `202` with body starting "Queued for a human:"
  (`TicketControllerTest#judgeGaveUpExceptionReturnsAcceptedWithHumanQueueMessage`).
- [x] Ticket that instructs the assistant (`injection` >= 0.5): refused before any draft
  (`ReplyDrafterTest#refusedTicketIsNeverDrafted`,
  `ReplyJudgeTest#screenInputRefusesInstructionsAimedAtTheAssistant`).
- [x] Empty reply: fails locally with no Jev call (`ReplyJudgeTest#emptyReplyFailsWithoutCallingJev`).
- [x] Each judge criterion just below its minimum fails
  (`ReplyJudgeTest#eachCriterionFailsBelowItsMinimum`).
- [x] Wrong content type: 4xx (`TicketControllerTest#submitWithUnsupportedMediaTypeReturnsClientError`).
- [ ] Not covered: a Jev or Anthropic call that errors or times out. The exception
  propagates as a 500; there is no test and no fallback.

## Rollback

Stateless: `git revert 710a7ee` returns to the `spring-ai-typesafe` version. No data,
schema or config outside the repo.

## Progress

- 2026-11-20: recorded retrospectively. Open: decide whether model-call errors should
  map to `202 Queued for a human` like a rejected reply (see `docs/progress.md`).
