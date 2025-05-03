# Dagger 2 for modularity

## Status

Adopted

## Decision

For better modularity and a more consistent API to implementors, the curator framework uses [Dagger 2](https://dagger.dev) as its dependency injection framework, and will require developers to use it too. 

## Context

Up to July 24, 2024 this system used Guice as its DI framework. Dagger 2 promises to be better in a few aspects:

- It does its work at compile-time, while Guice does it at run-time.
  - Errors show up during compilation, not at start-up of the code, therefore earlier.
- It generates (readable) code, which means:
  - No reflection, therefore faster, especially at start-up.
  - Shorter, simpler stacktraces.
  - Less memory usage.
- It has a smaller footprint:
  - The run-time JAR is just 34 kB. 
  - No Guava; yeay!

## Consequences

The consequences are the same as for ADR0004: developers building their own curators will need to migrate. The reason that I picked Guice at the time was that I wasn't aware of Dagger 2. Since then, I've learned that Dagger 2 is now preferred over Guice by many.
