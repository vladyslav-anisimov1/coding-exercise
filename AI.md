# AI Usage

AI assistance was used throughout the task for requirements analysis, low-level design review, implementation, test design, and code-quality checks. The final design and code were reviewed against the supplied PDF and `requirements.md`.

## Context and artifacts

- Functional source: `ODDS and Data - JAVA Coding Task.pdf`
- Project constraints: `requirements.md`
- Design artifact: `LLD.md`
- Implementation artifacts: Java source, Maven configuration, and JUnit tests in this repository

## Prompt history summary

1. Read the supplied PDF and project `requirements.md`; distinguish functional instructions from implementation preferences and prepare an LLD after resolving questions.
2. Resolve team-name normalization, case-insensitive uniqueness, score replacement semantics, finished-match errors, and the `LLD.md` filename.
3. Create the low-level design covering immutable snapshots, repository abstraction, bounded history, ordering, and concurrency.
4. Implement the design incrementally with meaningful Git commits.
5. Add unit, functional, and concurrency tests, then run Maven verification.

## Decisions influenced by AI review

- Reuse a monotonic `AtomicLong` value for both match identity and deterministic start ordering.
- Keep public `Match` objects immutable and use internal per-match synchronization for updates and finish operations.
- Use an active-team index to enforce case-insensitive uniqueness without a team dictionary.
- Keep creation coordination and history synchronization narrow instead of using one global lock for every operation.
