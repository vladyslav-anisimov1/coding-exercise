# Implementation Requirements

The attached PDF is the source of truth for the functional requirements of the task. 
This file adds my implementation preferences and architectural constraints. 
Do not invent new business requirements that are not present in the PDF or this document.
If something is ambiguous, point it out instead of silently making up a rule. 

At the same time, do not treat my technical suggestions as unquestionable requirements. 
Challenge them when there is a simpler or better solution, explain the alternatives and their trade-offs, 
and recommend what you think is the best option. Technical alternatives are welcome; invented business requirements are not.

## General approach

Keep the project simple. This is a small Java library and should not turn into an enterprise application. 
Avoid unnecessary abstractions, excessive layering, infrastructure, frameworks, or dependencies. 
Do not add REST APIs, controllers, databases, JPA, Docker, messaging, or similar infrastructure.
Follow the Google Java Style Guide. 

## Project structure

Use three main logical layers/packages:

- domain — the domain model, kept as small and simple as possible.
- persistence — storage abstractions and the in-memory implementation.
- service — the scoreboard functionality described in the PDF.

The service layer should not depend directly on a particular storage implementation. 
The persistence layer should expose an abstraction that allows the current in-memory implementation to be replaced 
later by another implementation, for example a database-backed repository, without rewriting the service logic.
Do not overengineer this abstraction. It should remain small and focused on what the scoreboard actually needs.
The library should not depend on Spring, but it should be easy to integrate into a Spring/Spring Boot application later, 
for example through constructor injection and Spring configuration that creates the library components as beans.

## Match storage

Active matches should be stored in memory. My initial preference is a ConcurrentHashMap, 
but evaluate whether this is the best option and explain reasonable alternatives if there are any.
Finished matches should be moved to a separate history structure.
For history, use a bounded e.g. LinkedHashMap that keeps at most the 20 most recently finished matches. 
When the limit is exceeded, the oldest entry should be evicted.
The history is intentionally small and exists mainly to support lookup of recently finished matches.
Please consider thread safety of this structure as part of the overall concurrency design.

## Match identity and ordering

Each match should have a unique identifier. My initial idea is to generate IDs using an AtomicLong.
A match should also contain its start timestamp.
However, summary ordering should not depend purely on timestamps, because timestamps may not provide 
a reliable deterministic ordering when matches are started very close to each other.
I would like to have an internal monotonic ordering value representing the order in which matches were started.
One possible approach is a separate sequence number.
Another possibility is to use the generated match ID itself as the ordering sequence if the ID generation guarantees the required ordering.
Before implementing this, compare these approaches and explain their advantages and disadvantages. 
Prefer the simplest approach that still gives deterministic behavior.
Also don't forget about constraints, there can't be duplicate matches and unique team can participate
only in one match in given moment of time.

## Domain model

Keep the domain model minimal. Do not introduce domain abstractions only for the sake of having more domain classes.
A match should contain the information needed by the task, including its identity, teams, score, start timestamp 
and whatever internal ordering information we decide to use.
Scores must never be negative.
Consider immutable domain objects if they make concurrency and reasoning simpler, but explain the trade-off before committing to the design.

## Error handling

Keep error handling simple.
Instead of creating a large exception hierarchy, use one generic scoreboard/domain exception with 
a specific error code describing the failure.
For example, different error codes may represent cases such as an unknown match, invalid score or invalid operation.
Do not invent error cases unnecessarily. Error codes should correspond to actual validation rules or behavior 
that we explicitly decide to support.

## Additional operation

The PDF requires exactly one additional operation. The additional operation should be: getMatch(matchId)
It should return a match regardless of whether the match is currently active or has already finished, 
as long as the finished match is still available in the bounded history.
This feature should be implemented only after all functionality explicitly required by the PDF is complete.
It should also be introduced in its own Git commit, as requested by the exercise.

## Thread safety

The library should be thread-safe.
I want a simple but reasonably efficient concurrency model.
Avoid solving thread safety by placing one global lock around every read and write operation.
My initial direction is:

- AtomicLong for ID/order generation;
- ConcurrentHashMap for active matches;
- atomic map operations where useful.

However, treat this as a proposal, not a mandatory implementation.
Analyze the actual race conditions first and explain possible approaches before choosing one.
In particular, think about:

- concurrent updates of different matches;
- concurrent updates of the same match;
- update versus finish for the same match;
- concurrent match creation;
- summary reads while matches are being modified;
- moving a finished match from active storage into history.

Explain what consistency guarantees the chosen design provides.
The goal is practical thread safety for this exercise, not a complicated locking framework.

## Testing

Testing is an important part of the solution.
Include both unit tests and higher-level functional tests.
Unit tests should cover normal behavior, validation, ordering, error handling and edge cases.
Functional tests should exercise the real service and persistence implementations together.
Concurrency should receive particular attention. Add multi-threaded tests for meaningful race conditions and thread-safety guarantees.
Concurrency tests should be deterministic where reasonably possible. Prefer synchronization primitives such 
as CountDownLatch, CyclicBarrier, Phaser or similar tools instead of relying on arbitrary Thread.sleep() calls.
Tests should verify observable behavior rather than implementation details.

## Before implementation

Do not immediately generate the full implementation.
First read the PDF and this document, then propose a short implementation plan.
Highlight any ambiguities or architectural decisions that still need to be resolved.
For important technical decisions, especially match identity/order generation, immutable vs mutable matches, 
repository design, history synchronization and concurrency strategy, provide reasonable alternatives 
with pros and cons and give your recommendation.
Ask me questions where my decision would affect the implementation.
Once those questions are resolved, update the plan.
Only then proceed with the implementation incrementally, keeping each step small and reviewable.