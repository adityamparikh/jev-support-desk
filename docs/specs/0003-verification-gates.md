# Spec 0003: Verification gates: CI, style, dependency watch

- Status: In progress (branch `aine-readiness`, 2026-11-20)
- Related: ADR 0004, `AINE-SETTINGS-TODO.md`

## Goal

One command, `./mvnw install`, runs every check this repo has (enforcer, format,
checkstyle, compile, tests), and GitHub runs the same command before a change lands.

## Non-goals

- No deploy or release pipeline: nothing is deployed.
- No live-API integration tests in CI: CI has no keys and must never spend.
- No coverage threshold.

## Acceptance criteria

- [ ] AC1 `./mvnw install` with `TYPESAFE_API_KEY`, `ANTHROPIC_API_KEY` unset exits 0.
- [ ] AC2 It fails on a formatting violation (`spring-javaformat:validate`) and on a
  checkstyle violation (`maven-checkstyle-plugin:check`), both bound to `validate`.
- [ ] AC3 `.github/workflows/ci.yml` runs `./mvnw -B install` on Temurin 21 on
  `pull_request`, on `push` to `main`, nightly (`schedule`) and on `workflow_dispatch`,
  with no secrets in its environment.
- [ ] AC4 The same workflow runs gitleaks over the full history.
- [ ] AC5 `.github/dependabot.yml` covers `maven` (`/`) and `github-actions`, weekly.

## Failure cases

- A snapshot dependency added to `pom.xml`: the enforcer fails the build at `validate`.
- A future snapshot or upstream release breaking the build with no commit here: the
  nightly run goes red.
- A key committed by mistake: the gitleaks job fails the pull request.

## Rollback

Revert the commit; the workflow and plugin config are self-contained. Branch protection
(once set per `AINE-SETTINGS-TODO.md`) is removed with
`gh api -X DELETE repos/adityamparikh/jev-support-desk/branches/main/protection`.

## Progress

- 2026-11-20: spec written; implementation in progress on `aine-readiness`.
