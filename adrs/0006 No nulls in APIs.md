# No nulls in APIs

## Status

Adopted

## Decision

APIs - interfaces and classes that are exposed to other components - never accept or return `null`, unless this is documented explicitly.

## Context

Sir Tony Hoare:

> “I call it my billion-dollar mistake. It was the invention of the null reference in 1965… This has led to innumerable errors, vulnerabilities, and system crashes, which have probably caused a billion dollars of pain and damage in the last forty years.”

The easiest way to tackle many programming errors is to not use `null` at all. That can be overkill, especially with private methods or in isolated blocks of code. So, there it is allowed. 

I've been applying ADR since the start of this project (and actually: in all my projects, since I don't know when), but I never wrote it down.

## Consequences

The main consequence is that the code is less error-prone. No guessing if `null` is allowed as a parameter somewhere, of if `null` can be returned.

Two other consequences are:

- The use of `Optional` as a field or parameter. This is considered bad practice, but sometimes there's no better way. Especially in the case of Dagger, which supports optional injection.
- The use of "null objects" for result values.
