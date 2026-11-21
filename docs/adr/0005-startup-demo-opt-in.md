# ADR 0005: The startup demo is opt-in

- Status: Accepted (2026-11-20, branch `aine-readiness`; spec
  [0002](../specs/0002-safe-by-default-startup.md))

## Context

`DemoRunner` triages and drafts `supportdesk.demo.ticket` at startup. It was on by
default (`supportdesk.demo.enabled=true` and `matchIfMissing = true`), so every
`./mvnw spring-boot:run` with keys in the environment made paid TypeSafe and Anthropic
calls (up to 1 triage + 1 screen + 3 drafts + 3 judge calls) with nobody asked. Coding
agents run that command routinely.

## Decision

- `supportdesk.demo.enabled=false` in `src/main/resources/application.properties`, and
  `matchIfMissing = false` on `DemoRunner`, so the demo runs only when someone sets the
  flag to `true` on purpose.
- CLAUDE.md lists the operations an agent must ask before running, and
  `.claude/settings.json` makes Claude Code prompt for them.

## Consequences

- Readers of the article add `--supportdesk.demo.enabled=true` (README shows how) to get
  the log block they paste back.
- Starting the app with keys still allows paid calls through `POST /tickets`; that route
  is fenced by the ask-before rules, not by code.
