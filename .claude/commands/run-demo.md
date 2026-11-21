---
description: Run the startup demo once against the live APIs and capture the article numbers (paid; asks first)
---

This makes paid calls: 1 Jev triage, 1 Jev input screen, then up to 3 Anthropic drafts
and 3 Jev judge calls. Do not start until the user has said yes in this conversation to
this run.

1. Tell the user the cost above and ask for a yes. Stop if there is none.
2. Build first with the keys unset, so a broken build does not waste a paid run:
   `env -u TYPESAFE_API_KEY -u ANTHROPIC_API_KEY ./mvnw -q install`.
3. Check the keys are present without printing them:
   `test -n "$TYPESAFE_API_KEY" && test -n "$ANTHROPIC_API_KEY" && echo keys-set`.
   If either is missing, ask the user to set them (see `.env.example`); never read
   `.env` or `.idea/workspace.xml` yourself.
4. Run once, in the background, with the demo enabled:
   `./mvnw spring-boot:run -Dspring-boot.run.arguments=--supportdesk.demo.enabled=true`.
5. Wait for the log block that starts with `Ticket       :` and ends with `action`, then
   the "Approved draft" line or the "Judge rejected the reply" / "Escalated" line.
6. Stop the app. Do not send `POST /tickets` or anything from `tickets.http`: each one is
   another paid run and needs its own yes.
7. Give the user the log block and the draft verbatim (they contain no keys), and add a
   dated line to `docs/progress.md` with the model, the action and decision, and the
   Spring Boot / Spring AI versions it ran on.
