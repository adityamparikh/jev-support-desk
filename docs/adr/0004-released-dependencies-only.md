# ADR 0004: Released dependencies only; no snapshot BOM

- Status: Accepted (2026-11-20, branch `aine-readiness`)

## Context

`pom.xml` imported the Spring AI BOM at `2.1.0-SNAPSHOT` from `repo.spring.io/snapshot`.
Snapshot build 37 (2026-11-15) dropped `typesafe-java-sdk`, so
`org.springaicommunity.typesafe.TypeSafeClient` vanished and `./mvnw install` failed to
compile `JevConfig` on a clean checkout, without any commit to this repo. Pinning
`typesafe-java-sdk:0.4.0` back would not have fixed it: the new
`SystemOneDecisionModel.builder` takes `org.springframework.ai.systemone.api.SystemOneClient`.
Spring AI 2.1.0 was released to Maven Central on 2026-11-19.

## Decision

- Import the released Spring AI `2.1.0` BOM; remove the snapshot repository.
- `JevConfig` builds a `SystemOneClient`.
- `maven-enforcer-plugin` runs `requireReleaseDeps` (and `requireJavaVersion [21,)`) in
  every build, so a snapshot dependency fails `./mvnw install` instead of drifting in.
- CI also runs on a nightly schedule, so drift in anything still resolved by range or by
  the network is seen the day it happens.

## Consequences

- Upgrades are explicit commits (Dependabot proposes them weekly).
- Spring AI 2.1.0 is built against Spring Boot 4.2 / Spring Framework 7.1, and the
  app is on Boot 4.2.0-RC2, so the two lines match. `requireReleaseDeps` allows release
  candidates; it only rejects snapshots.
