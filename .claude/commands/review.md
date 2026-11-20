---
description: Review the current change against this repo's invariants before it lands
argument-hint: "[base ref, default main]"
allowed-tools: Bash(git diff:*), Bash(git log:*), Bash(git status:*), Read, Grep, Glob
---

Review the change on this branch against `${ARGUMENTS:-main}`. Get it with
`git diff ${ARGUMENTS:-main}...HEAD` plus `git diff` for uncommitted work, and read each
touched file in full, not only the hunks.

Look for real problems, most severe first. For each finding give `file:line`, what is
wrong, why it matters here, and the fix. Skip style nits that spring-javaformat or
checkstyle would catch: `./mvnw install` already enforces them.

Check every one of these; they are the rules in CLAUDE.md and `docs/adr/` that code
review is the only guard for:

1. **Money and keys.** Nothing sets `supportdesk.demo.enabled=true` in a committed file,
   adds a startup path that calls `DecisionModel` or `ChatClient`, or puts a real call in
   a test. No key value, `.env` or `.idea/` file is added. Tests still pass with
   `TYPESAFE_API_KEY` and `ANTHROPIC_API_KEY` unset.
2. **Thresholds.** A change to the gate (floor 0.60, `auto_close` 0.90), the judge
   minimums (0.7 / 0.8 / 0.8 / tone 1.5, injection 0.5) or `MAX_ATTEMPTS = 3` comes with
   updated boundary cases in `ConfidenceGateTest`, `TriageServiceTest` or
   `ReplyJudgeTest`, and an ADR update (0002 or 0003).
3. **No best-effort replies.** `ReplyDrafter` never returns a draft the judge did not
   pass; `ReplyRejectedException` still maps to `202 Accepted` "Queued for a human: ...";
   `ESCALATE` still returns `draft: null` without calling the chat model.
4. **Exact rules in code.** A deterministic check (empty, length, regex, allow-list) is
   plain Java in `ReplyJudge`, not a new Jev question.
5. **Question order.** `TicketQuestions.TRIAGE` and the `ReplyJudge` maps stay ordered
   (`LinkedHashMap`); System One sends questions in map order.
6. **Port 8080** changed in `application.properties`, `README.md` and `tickets.http`
   together, or not at all.
7. **DemoRunner log block** unchanged unless the change says why (the article quotes it).
8. **Dependencies.** No snapshot versions or snapshot repositories (the enforcer also
   fails on these); a Spring AI or Boot bump updates CLAUDE.md's stack line.
9. **Docs still true.** CLAUDE.md, README.md and the spec the commit references still
   describe the code. A non-trivial change has a spec in `docs/specs/` and the commit
   message names it.

End with a verdict: `ready`, `ready with nits`, or `needs changes`, and the one thing to
fix first.
