# jev-support-desk

Companion project for **Springing into Jev**. One Spring Boot app that triages a support
ticket with Jev, gates routing on confidence, and drafts a reply with a chat model that a
Jev judge and guardrail must pass.

> **`ollama` branch:** every System One call (triage, judge, guardrails) goes to a local
> [Ollama](https://spring-ai-community.github.io/spring-ai-typesafe/latest-snapshot/client/Ollama/)
> decision model instead of hosted Jev. Ollama 0.35+ serves the same `/v1/systemone` protocol,
> so the Java code is unchanged; only `application.properties` points elsewhere. The reply is
> still drafted by Anthropic.

Spring Boot 4.0.7 · Spring AI 2.0.1 · Spring AI TypeSafe 0.2.0 · Java 21

## Start Ollama

Ollama 0.35 or later:
```bash
ollama pull nimble        # 9B, a 9.5 GB download; or tev1 (4B), tev1:0.8b
```

Ollama serves on `http://localhost:11434` and loads the model on the first request, so the app
allows a 60 s timeout. Override the defaults with `TYPESAFE_BASE_URL`, `TYPESAFE_API_KEY` and
`TYPESAFE_DEFAULT_MODEL` (for example `TYPESAFE_DEFAULT_MODEL=tev1`).

Expect different numbers from Jev. The gate (floor 0.60, `auto_close` 0.90) was tuned on Jev.
In the TypeSafe project's own comparison, nimble was confident on triage but sent a refund ticket
to `auto_close`. Also, on Ollama three questions in one call (as `TriageService` does) are slower
than three separate calls, the opposite of Jev. Measure before retuning.

## Run in IntelliJ IDEA

1. **File → Open** this folder. IntelliJ imports the Maven project; use a Java 21 SDK.
2. Open `SupportDeskApplication`, click the gutter ▶, then **Modify Run Configuration**.
3. Under **Environment variables** add:
   ```
   ANTHROPIC_API_KEY=<your Anthropic key>
   ```
   Optional: `ANTHROPIC_MODEL` (defaults to `claude-sonnet-5`), `TYPESAFE_DEFAULT_MODEL`.
4. Run. On startup the app triages the payouts ticket and logs the results and the approved draft.
5. With the app running, open `tickets.http` and click ▶ next to a request.

From a terminal instead:
```bash
export ANTHROPIC_API_KEY=...
./mvnw spring-boot:run
curl -X POST localhost:8080/tickets -H 'Content-Type: text/plain' \
     -d 'Help! My payouts have been failing for 3 days.'
```

If `ANTHROPIC_API_KEY` is missing, startup fails with an unresolved placeholder. That is intended.

To skip the startup demo, set `supportdesk.demo.enabled=false`. For a code-level walkthrough
of the triage and drafting pipeline, see [CLAUDE.md](CLAUDE.md).

## For the article

Paste back the startup log block (Ticket … action) plus the approved draft, and the JSON
from `POST /tickets`. Neither contains your keys.
