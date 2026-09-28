# jev-support-desk

Companion project for **Springing into Jev**. One Spring Boot app that triages a support
ticket with Jev, gates routing on confidence, and drafts a reply with a chat model that a
Jev judge and guardrail must pass.

Spring Boot 4.0.7 · Spring AI 2.0.1 · Spring AI TypeSafe 0.2.0 · Java 21

## Run in IntelliJ IDEA

1. **File → Open** this folder. IntelliJ imports the Maven project; use a Java 21 SDK.
2. Open `SupportDeskApplication`, click the gutter ▶, then **Modify Run Configuration**.
3. Under **Environment variables** add:
   ```
   TYPESAFE_API_KEY=<your TypeSafe key>;ANTHROPIC_API_KEY=<your Anthropic key>
   ```
   Optional: `ANTHROPIC_MODEL` (defaults to `claude-sonnet-5`).
4. Run. On startup the app triages the payouts ticket and logs the results and the approved draft.
5. With the app running, open `tickets.http` and click ▶ next to a request.

From a terminal instead:
```bash
export TYPESAFE_API_KEY=...
export ANTHROPIC_API_KEY=...
./mvnw spring-boot:run
curl -X POST localhost:8080/tickets -H 'Content-Type: text/plain' \
     -d 'Help! My payouts have been failing for 3 days.'
```

If either variable is missing, startup fails with an unresolved placeholder. That is intended.

To skip the startup demo, set `supportdesk.demo.enabled=false`. For a code-level walkthrough
of the triage and drafting pipeline, see [CLAUDE.md](CLAUDE.md).

## For the article

Paste back the startup log block (Ticket … action) plus the approved draft, and the JSON
from `POST /tickets`. Neither contains your keys.
