# Specs

Forward-looking specs: what a change must do, how to check it, how to undo it. Decisions
and their reasons go in `docs/adr/`; in-flight state across sessions goes in
`docs/progress.md`.

- Copy `TEMPLATE.md` to `NNNN-short-name.md` (next number) before starting a change.
- Every acceptance criterion names the test or command that checks it.
- Commit messages for the work reference the spec (`Spec: docs/specs/NNNN-...`).
- Set `Status` to `Shipped (<sha>)` when it lands.

| Spec | Title | Status |
|------|-------|--------|
| [0001](0001-triage-and-draft-pipeline.md) | Triage and draft pipeline | Shipped (`710a7ee`), written retrospectively |
| [0002](0002-safe-by-default-startup.md) | Safe-by-default startup and secrets hygiene | Shipped (branch `aine-readiness`) |
| [0003](0003-verification-gates.md) | Verification gates: CI, style, dependency watch | Shipped (branch `aine-readiness`); repo settings pending |
