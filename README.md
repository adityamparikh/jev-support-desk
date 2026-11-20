# jev-support-desk

Companion project for **Springing into Jev**. One Spring Boot app that triages a support
ticket with Jev, gates routing on confidence, and drafts a reply with a chat model that a
Jev judge and guardrail check must pass.

Spring Boot 4.2.0-RC2 · Spring AI 2.1.0 (System One decision model) · Java 21

## Run in IntelliJ IDEA

1. **File → Open** this folder. IntelliJ imports the Maven project; use a Java 21 SDK.
2. Open `SupportDeskApplication`, click the gutter ▶, then **Modify Run Configuration**.
3. Under **Environment variables** add:
   ```
   TYPESAFE_API_KEY=<your TypeSafe key>;ANTHROPIC_API_KEY=<your Anthropic key>
   ```
   Optional: `ANTHROPIC_MODEL` (defaults to `claude-sonnet-5`).
   To run the startup demo, also add `--supportdesk.demo.enabled=true` under **Program arguments**.
4. Run. With the demo enabled, the app triages the payouts ticket on startup and logs the
   results and the approved draft. Every demo run and every `POST /tickets` makes paid
   TypeSafe and Anthropic calls.
5. With the app running, open `tickets.http` and click ▶ next to a request.

From a terminal instead:
```bash
cp .env.example .env            # then fill in the two keys; .env is git-ignored
set -a; source .env; set +a
./mvnw spring-boot:run -Dspring-boot.run.arguments=--supportdesk.demo.enabled=true
curl -X POST localhost:8080/tickets -H 'Content-Type: text/plain' \
     -d 'Help! My payouts have been failing for 3 days.'
```

If either variable is missing, startup fails with an unresolved placeholder. That is intended.

The startup demo is off by default (`supportdesk.demo.enabled=false`), so a plain
`./mvnw spring-boot:run` makes no calls until a ticket is posted. For a code-level walkthrough
of the triage and drafting pipeline, see [CLAUDE.md](CLAUDE.md).

## Build and test

```bash
./mvnw install
```

Runs every check (dependency rules, compile, all tests) and needs no API keys: the tests
mock both models. Design notes and decisions are in [docs/adr](docs/adr), specs in
[docs/specs](docs/specs).

## For the article

Paste back the startup log block (Ticket … action) plus the approved draft, and the JSON
from `POST /tickets`. Neither contains your keys.
