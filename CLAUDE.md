# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Companion demo for the article **"Springing into Jev"**. One Spring Boot app that triages a support ticket with Jev (TypeSafe's System One model), gates the routing decision on confidence, and drafts a reply with an Anthropic chat model. A Jev judge and guardrail must approve that reply.

Stack: Spring Boot 4.0.7 · Spring AI 2.0.1 (BOM) · Spring AI TypeSafe 0.2.0 (`org.springaicommunity`) · Java 21 · Maven wrapper.

## Commands

```bash
./mvnw install                      # full build + tests (required after any change)
./mvnw spring-boot:run              # run the app (needs env vars below)
./mvnw test -Dtest=ClassName#method # single test
```

On this `ollama` branch, System One calls go to a local Ollama decision model instead of hosted Jev (see README for setup). `TYPESAFE_BASE_URL` (default `http://localhost:11434`), `TYPESAFE_API_KEY` (default `ollama`) and `TYPESAFE_DEFAULT_MODEL` (default `nimble`; Ollama has no `jev-latest`) are optional. `ANTHROPIC_API_KEY` is still required, and if it is missing, startup fails on purpose with an unresolved placeholder. `ANTHROPIC_MODEL` is optional and defaults to `claude-sonnet-5`. Ollama rejects a Noul without `instructions` (400), so give every Noul instructions.

The app listens on port 8080 (`server.port` in `application.properties`). `README.md` and `tickets.http` also use 8080, so change all three together.

Tests run without API keys. `src/test/resources/application.properties` supplies mock-key fallbacks and sets `supportdesk.demo.enabled=false`, so `DemoRunner` makes no live calls. Unit tests mock `TypeSafeClient` / `ChatClient`, using `Answers.RETURNS_DEEP_STUBS` to stub the fluent `prompt().user(..).call().content()` chain. `TicketControllerTest` is a `@WebMvcTest` that uses `@MockitoBean`.

## Architecture

Everything is in `com.example.supportdesk`. A request goes through two stages:

1. **Triage** (`TriageService`): a single `typeSafe.systemOne(ticket, TicketQuestions.TRIAGE)` call asks three typed questions together: `is_urgent` (Noul, yes/no probability), `department` (Choice), and `frustration` (Score). Plain code then combines two of the answers into an action: `auto_close` if urgency < 0.2 and frustration < 0.5, otherwise `route_to_<department>`. `JevConfidenceGate` (floor 0.60; `auto_close` needs 0.90) turns the department confidence into a decision: `EXECUTE`, `CONFIRM` or `ESCALATE`.
2. **Draft** (`SupportReplyConfig`): the `supportChatClient` bean wraps the Anthropic `ChatClient` in two advisors:
   - `JevSelfRefineAdvisor`: re-drafts up to 3 times until the `supportReplyJudge` (`JevJudge`) passes. `failOnExhaustedAttempts(true)` makes it throw `JevSelfRefineFailedException` instead of returning its best attempt.
   - `JevGuardrailAdvisor`: the default input and output guardrails.

   The judge combines weighted Jev questions (`addresses_issue`, `no_unapproved_promise`, `tone`) with deterministic `.check(...)` lambdas. The lambdas run locally and are never sent to Jev. Put exact rules in `.check`, not in Jev questions.

Entry points that use both stages:
- `TicketController`: `POST /tickets` (`text/plain` body). `ESCALATE` returns no draft (`draft: null`). `JevSelfRefineFailedException` becomes `202 Accepted` with "Queued for a human: …".
- `DemoRunner`: a `CommandLineRunner` that triages and drafts `supportdesk.demo.ticket` at startup and logs the numbers the article quotes. It is gated by `supportdesk.demo.enabled`. Don't change the log block format without a reason: README asks readers to paste it back for the article.

`spring.ai.typesafe.api-key` must be set in `application.properties`. The starter's auto-configuration is conditional on that property, so setting the environment variable alone does not create `TypeSafeClient`.

## Conventions

- Spring Framework source style: tab indentation, a blank line after the class declaration and before the closing brace, and a separate import group for `org.springframework.*`. Classes are package-private unless another class needs them.
- Use `org.apache.commons.logging.Log` / `LogFactory` for logging, as the existing code does.
- `tickets.http` has IntelliJ HTTP-client requests for manual testing.
