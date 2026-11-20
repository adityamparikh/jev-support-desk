# Spec 0002: Safe-by-default startup and secrets hygiene

- Status: In progress (branch `aine-readiness`, 2026-11-20)
- Related: ADR 0005

## Goal

Starting the app, which agents do routinely, spends nothing unless a human opted in, and
the live API keys have no path into git.

## Non-goals

- Not removing `DemoRunner` or changing its log block (README asks readers to paste it).
- Not adding authentication or rate limiting to `POST /tickets`.
- Not rotating or scoping the existing keys: that is done in the TypeSafe and Anthropic
  consoles by the key owner.

## Acceptance criteria

- [ ] AC1 With no `supportdesk.demo.enabled` property, `DemoRunner` is not created.
  `DemoRunnerTest#demoRunnerNotConfiguredWhenPropertyMissing`.
- [ ] AC2 The shipped `src/main/resources/application.properties` leaves it off.
  `DemoRunnerTest#shippedApplicationPropertiesKeepDemoOff`.
- [ ] AC3 `supportdesk.demo.enabled=true` still creates it.
  `DemoRunnerTest#demoRunnerConfiguredOnlyWhenEnabled`.
- [ ] AC4 `git check-ignore .env .env.local .idea/workspace.xml secrets.pem` prints all
  four; `git check-ignore .env.example` prints nothing.
- [ ] AC5 `.env.example` lists `TYPESAFE_API_KEY`, `ANTHROPIC_API_KEY`, `ANTHROPIC_MODEL`
  with empty or default values only.
- [ ] AC6 CLAUDE.md has an "Ask before" list naming `./mvnw spring-boot:run` with real
  keys, `tickets.http`, `curl .../tickets`, and printing or committing keys; and
  `.claude/settings.json` sets those commands to `ask`.

## Failure cases

- Flag set to anything other than `true` (`yes`, `TRUE `, empty): demo stays off
  (`havingValue = "true"`; covered for `false` and missing by AC1 and
  `DemoRunnerTest#demoRunnerNotConfiguredWhenDisabled`).
- Keys missing at startup: startup still fails on the unresolved placeholder, as before.

## Rollback

Revert the commit, or set `supportdesk.demo.enabled=true` to restore the old startup
behaviour without a revert.

## Progress

- 2026-11-20: spec written; implementation in progress on `aine-readiness`.
