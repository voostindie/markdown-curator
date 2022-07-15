# Guice for modularity

## Status

Proposed

## Decision

For better modularity and a more consistent API to implementors, the curator framework will use Google Guice as its dependency injection framework, and will require developers to use it. 

## Context

This framework is modular, but it's (getting) a bit clunky:

- Implementors must extend the `CuratorTemplate`. This exposes more internals to them than needed and wanted.
- The `Query` registry is basically a plugin registry, but with a custom API.
- `DataModel`s are internally kept in yet another custom registry.
- Injecting data models into queries is done with a low-level injection mechanism.

With more and more extensions on the way (I have some ideas...) it's time to replace all this with a single, consistent solution: Google Guice.

Instead of registering a custom `CuratorFactory`, developers will be required to register a `CuratorModule`, which is a Guice module with some required overrides. In the model, implementors can register queries and data models at will, using standard Guice annotations and methods. 

## Consequences

Developers are not doing plain Java anymore; they are confronted with Google Guice, which they *must* use. I feel this is ratified because it:

- is a well-known dependency injection framework.
- supports standard dependency injection patterns.
- brings consistency to the curator APIs.
- gives more flexibility for configuring a `Curator`, queries, data models and other future pluggable types.

Lastly, but certainly not least: who are we kidding here? "Developers using the framework". That would be me, myself and I. And I'm perfectly fine with this.

## Options considered

1. **Google Guice** 
2. No dependency injection framework at all (current setup)
3. Some other dependency injection framework

### Google Guice

### Pros

Simple, small, light library with a very clear purpose: dependency injection. It's well known.

### Cons

Curator implementors must use Guice; they do not have the choice not to. Guice is not just an implementation detail, of the curator framework internally, it's also intentionally exposed. This takes away some freedom.

### No dependency injection framework at all (current setup)

### Pros

Plain Java. No tricks. No knowledge required except from Java and the curator APIs. It's simple.

### Cons

The current setup is reaching its limits. `DataModel` injection is a bit of a kludge. Also I have ideas on extensions that require better modularity.

### Some other dependency injection framework

### Pros

The obvious one to look at is Spring. That's probably the most popular dependency injection framework in the world, and therefore better known.

Other frameworks are - as far as I'm aware - less familiar. Guice is then a fine choice.

### Cons

Spring comes with many nuts and bolts. Too many for my taste in this particular use case. I like something simpler.

Other frameworks aren't better known than Guice, I believe.