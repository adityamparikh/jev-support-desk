# jev-support-desk

Companion project for **Springing into Jev**. One Spring Boot app that triages a support
ticket with Jev, gates routing on confidence, and drafts a reply with a chat model that a
Jev judge and guardrail must pass.

> **`laya` branch:** every System One call (triage, judge, guardrails) goes to a local
> [Laya](https://spring-ai-community.github.io/spring-ai-typesafe/latest-snapshot/client/Laya/)
> server instead of hosted Jev. Laya is an Apache-2.0 System One engine that serves the same
> `/v1/systemone` protocol, so the Java code is unchanged; only `application.properties` points
> elsewhere. The reply is still drafted by Anthropic.

Spring Boot 4.0.7 · Spring AI 2.0.1 · Spring AI TypeSafe 0.2.0 · Java 21

## Start laya-serve

```bash
uv venv --python 3.10
uv pip install "laya[serve]"

LAYA_DEVICE=mps \
LAYA_MODELS=english \
LAYA_PRELOAD=1 \
LAYA_API_KEY=local-test \
LAYA_PORT=8002 \
.venv/bin/laya-serve
```

Use `LAYA_DEVICE=cuda` or `cpu` off Apple Silicon. Check it with `curl -s localhost:8002/health`.
The app defaults to `http://localhost:8002` with key `local-test`; override with
`TYPESAFE_BASE_URL` and `TYPESAFE_API_KEY`.

Expect different numbers from Jev. Laya's confidences sit closer to 0.5 and it misses subtler
signals that need world knowledge, so more tickets may land on `CONFIRM` or `ESCALATE` under the
Jev-tuned gate (floor 0.60, `auto_close` 0.90). Measure before retuning.

## Run in IntelliJ IDEA

1. **File → Open** this folder. IntelliJ imports the Maven project; use a Java 21 SDK.
2. Open `SupportDeskApplication`, click the gutter ▶, then **Modify Run Configuration**.
3. Under **Environment variables** add:
   ```
   ANTHROPIC_API_KEY=<your Anthropic key>
   ```
   Optional: `ANTHROPIC_MODEL` (defaults to `claude-sonnet-5`), `TYPESAFE_BASE_URL`, `TYPESAFE_API_KEY`.
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
