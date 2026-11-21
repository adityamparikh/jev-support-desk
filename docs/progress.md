# Progress

Durable state for work that spans sessions: what was decided, what is open, what is
next. Read this at the start of a session; update it before you end one. Decisions with
lasting reasons move to `docs/adr/`; planned changes get a spec in `docs/specs/`.

## Done

- 2026-11-20, branch `aine-readiness`: build green again on Spring AI 2.1.0 (released)
  with `SystemOneClient` (ADR 0004); ADRs 0001-0003 and spec 0001 recorded
  retrospectively; spec 0002 shipped (demo opt-in, `.env` ignore rules and
  `.env.example`, ask-before list, `.claude/settings.json`); spec 0003 shipped in the
  repo (CI workflow, spring-javaformat, checkstyle, gitleaks, Dependabot, `.sdkmanrc`,
  wrapper SHA-256). The workflow has not run on GitHub yet: nothing is pushed.
  `.claude/commands/review.md` and `run-demo.md` added.

## Open questions (owner decides)

- Resolved: the base branch moved to Spring Boot 4.2.0-RC2, which matches the Boot 4.2 /
  Framework 7.1 line Spring AI 2.1.0 is built against. A live run
  (`--supportdesk.demo.enabled=true`) has not been repeated on it yet.
- Uncommitted Kotlin build config in the main checkout's `pom.xml`
  (`kotlin-maven-plugin` 2.2.0, no `src/main/kotlin` yet): intended or abandoned? If it
  lands, add Kotlin to CLAUDE.md's stack line and check it against the checkstyle and
  spring-javaformat setup.
- Model-call errors (Jev or Anthropic timeout or 5xx) surface as HTTP 500. Map them to
  `202 Queued for a human` like a rejected reply? (spec 0001, failure cases.)
- Keys: confirm `TYPESAFE_API_KEY` and `ANTHROPIC_API_KEY` are demo-only and spend-capped,
  and keep them out of agent shells unless a live run is intended.

## Next

- Repo settings in `AINE-SETTINGS-TODO.md` (branch protection, required checks, secret
  scanning, push protection) are waiting on the owner.
