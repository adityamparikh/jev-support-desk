# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Companion demo for the article **"Springing into Jev"**. One Spring Boot app that triages a support ticket with Jev (TypeSafe's System One model), gates the routing decision on confidence, and drafts a reply with an Anthropic chat model. A Jev judge and input/output checks must approve that reply.

Stack: Spring Boot 4.2.0-RC2 · Spring AI 2.1.0-RC2 (BOM, a pre-release on Maven Central) with `spring-ai-systemone` (the System One `DecisionModel`) · Java 21 · Maven wrapper.

## Commands

```bash
./mvnw install                      # full build + tests (required after any change)
./mvnw spring-boot:run              # run the app (needs env vars below)
./mvnw test -Dtest=ClassName#method # single test
```

Required environment variables: `TYPESAFE_API_KEY` and `ANTHROPIC_API_KEY`. `ANTHROPIC_MODEL` is optional and defaults to `claude-sonnet-5`. If either key is missing, startup fails on purpose with an unresolved placeholder.

The app listens on port 8080 (`server.port` in `application.properties`). `README.md` and `tickets.http` also use 8080, so change all three together.

Tests run without API keys. `src/test/resources/application.properties` supplies mock-key fallbacks and sets `supportdesk.demo.enabled=false`, so `DemoRunner` makes no live calls. Unit tests mock `DecisionModel` / `ChatClient`, using `Answers.RETURNS_DEEP_STUBS` to stub the fluent `prompt().user(..).call().content()` chain. `TicketControllerTest` is a `@WebMvcTest` that uses `@MockitoBean`.

## Architecture

Everything is in `com.example.supportdesk`. The 2.1 System One module has no auto-configuration and no judge, advisor or gate classes, so `JevConfig` declares `TypeSafeClient` and `DecisionModel` beans and the rest is plain code on top of `DecisionModel`. A request goes through two stages:

1. **Triage** (`TriageService`): a single `decisionModel.call(...)` asks three typed questions together (`TicketQuestions.TRIAGE`): `is_urgent` (`YesNoQuestion`), `department` (`ChoiceQuestion`) and `frustration` (`ScoreQuestion`, 3 levels; the level value is normalised to 0..1). Plain code combines two of the answers into an action: `auto_close` if urgency < 0.2 and frustration < 0.5, otherwise `route_to_<department>`. `ConfidenceGate` (floor 0.60; `auto_close` needs 0.90) turns the department confidence into `EXECUTE`, `CONFIRM` or `ESCALATE`.
2. **Draft** (`ReplyDrafter`): drafts with the Anthropic `supportChatClient`, then asks `ReplyJudge` to review it. A failed review feeds the defects back and re-drafts, up to 3 attempts; then it throws `ReplyRejectedException` instead of returning its best attempt. Before drafting, `ReplyJudge.screenInput` refuses tickets that try to instruct the assistant.

   `ReplyJudge` combines Jev questions (`addresses_issue`, `no_unapproved_promise`, `no_leak`, `tone`, each with a minimum) with a deterministic empty-reply check that runs locally and is never sent to Jev. Put exact rules in code, not in Jev questions.

Entry points that use both stages:
- `TicketController`: `POST /tickets` (`text/plain` body). `ESCALATE` returns no draft (`draft: null`). `ReplyRejectedException` becomes `202 Accepted` with "Queued for a human: …".
- `DemoRunner`: a `CommandLineRunner` that triages and drafts `supportdesk.demo.ticket` at startup and logs the numbers the article quotes. It is gated by `supportdesk.demo.enabled`. Don't change the log block format without a reason: README asks readers to paste it back for the article.

`TypeSafeClient` is built from `spring.ai.typesafe.api-key`, which must be set in `application.properties`; setting the environment variable alone does not populate it.

## Conventions

- Spring Framework source style: tab indentation, a blank line after the class declaration and before the closing brace, and a separate import group for `org.springframework.*`. Classes are package-private unless another class needs them.
- Use `org.apache.commons.logging.Log` / `LogFactory` for logging, as the existing code does.
- `tickets.http` has IntelliJ HTTP-client requests for manual testing.
