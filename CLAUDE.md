# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test

Scala 3.3.4 project using sbt 1.10.6. Tests use munit.

```bash
sbt compile        # compile
sbt test           # run all tests
sbt "testOnly v4s.ValidatedSuite"       # run a single test suite
sbt "testOnly v4s.ValidatedSuite -- *zip*"  # run tests matching a pattern
```

## Architecture

v4s is a validation library providing error accumulation with optional short-circuiting. Single package `v4s`, single dependency: cats-core (for `NonEmptyList`).

### Core types (`Validated.scala`)

`Validated[+E, +A]` — sealed trait with two cases: `Valid[A]` and `Invalid[E]` (wraps `NonEmptyList[E]`). Provides `map`, `flatMap`, `zip` (accumulates errors from both sides), `fold`, conversions (`toOption`, `toEither`), and `sequence` on `List[Validated]`. `flatMap` short-circuits; `zip` accumulates.

### Scoped validation DSL (`ValidationScope.scala`)

`ValidationScope[E]` — mutable scope that collects errors. Two families of operations:
- **`ensure*`** — accumulating: adds errors but continues execution.
- **`demand*`** — short-circuiting: adds error and aborts via a scope-local exception.

Builder functions `validate[E]` (returns `Validated[E, Unit]`) and `validated[E, A]` (returns `Validated[E, A]`) create a scope and run a block using Scala 3 context functions (`ValidationScope[E] ?=>`).

Top-level DSL functions (`ensure`, `demand`, `ensureDefined`, `demandDefined`, `ensureValue`, `demandValue`, `demandValid`, `attach`) delegate to the given `ValidationScope`. Extension methods on scope provide `.get`, `.attachV()`, `value.ensure(pred)(err)`, `value.demand(pred)(err)`.

### Conversions (`conversions.scala`)

Extension methods on `Try[A]` to convert to `Validated`.

## Style Rules (apply to all output: code, comments, docs, commit messages)

- Be succinct. Say it once, say it short.
- No redundant comments. If the code is clear, don't comment it.
- No filler text, no restating the obvious, no "this function does X" before a function named X.
- When asked to "eliminate repetition" or "remove redundant comments", take it literally.
- No fluff, no fuzzy

## Before Writing Code

- Check if a rough design or architecture decision is needed first. Ask if unclear.
- Design around data structures. Get the data model right before implementing logic around it.
- Develop the critical path first — the hard, fundamental part stripped to essentials.
- Don't introduce abstractions preemptively. Duplication is cheaper than the wrong abstraction. Let patterns emerge.
- Think about module and package structure before creating new packages.
- Don't create fine-grained packages with one class each ("categoritis"). Organise by feature, not by category.
- Don't introduce DTOs if not needed. E.g. if kafka models are generated from an avro spec, you can map directly to domain models without any DTO.

## Writing Code

- One level of abstraction per function. Don't mix high-level orchestration with low-level details.
- Functions should fit on a screen (~80–100 lines max).
- Group code by functional cohesion (things that contribute to the same task), not by class-per-responsibility.
- Keep dependencies minimal. Don't add libraries for trivial tasks.
- No tactical DDD patterns or hexagonal architecture unless explicitly requested.
- If you don't know a library, read its docs or source on GitHub. Don't guess the API.

## Testing

- Write integration and e2e tests early. They catch what AI misses — AI reasons locally, tests verify globally.
- For UI: write Selenium e2e tests first. Use them to verify and self-correct.
- One test per desired external behavior, plus one test per bug.
- Tests target the API of a cohesive unit — not individual classes or internal methods.
- Use tests to find edge cases.
- Don't write tests before the implementation exists (no TDD).

## APIs and Interfaces

- Treat APIs as permanent. Don't change signatures without explicit approval.
- Be strict in what you accept and what you return. Don't silently tolerate malformed input.
- Minimize observable behavior surface — anything observable will be depended on.

## Conventions and Consistency

- Follow existing patterns in the codebase. When in doubt, match what's already there.
- Global project structure matters. Local style within a function or module is flexible.
- If a convention exists (naming, structure, patterns), follow it. Don't introduce alternatives.

## AI Workflow

- Don't over-engineer prompts or plans. Work with what's given plainly.
- After producing code, expect it to be reviewed and challenged. ~50% of output will need major changes.
- Never commit secrets, credentials, or API keys.
- When fixing bugs: reproduce with a test first, then fix.
- If a task is ambiguous, ask one clarifying question rather than guessing.
