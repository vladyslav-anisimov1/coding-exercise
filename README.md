# Live Football World Cup Scoreboard

Small thread-safe Java library implementing the Live Football World Cup Scoreboard coding task.

## Requirements

- Java 17+
- Maven 3.9+

Run the test suite with:

```shell
mvn test
```

## Usage

```java
ScoreboardService scoreboard =
    new ScoreboardService(new InMemoryMatchRepository());

Match match = scoreboard.startMatch(" spain ", "brazil");
scoreboard.updateScore(match.getMatchId(), 1, 0);
List<Match> summary = scoreboard.getSummary();
scoreboard.finishMatch(match.getMatchId());
```

`ScoreboardService` is the public façade. `MatchRepository` is the storage abstraction, and `InMemoryMatchRepository` is the current implementation.

## Behavior and assumptions

- Scores are replaced by `updateScore`; both values must be non-negative.
- Team names are trimmed, stored with their first character capitalized, and compared case-insensitively for active-match uniqueness.
- A team can participate in only one active match.
- Summary order is total score descending, then most recently started match first.
- Active matches are stored in a `ConcurrentHashMap`; updates to one match are serialized without blocking unrelated matches.
- Finished matches are retained in a synchronized, insertion-ordered history containing at most 20 entries.
- Returned `Match` objects are immutable snapshots. A summary may contain entries captured at slightly different moments during concurrent updates, but no returned object changes after it is returned.
- Updating or finishing an already-finished match fails.

See [LLD.md](LLD.md) for the detailed design, concurrency model, alternatives, and test strategy.

## Project structure

```text
src/main/java/com/example/scoreboard
├── domain
├── persistence
└── service
```
