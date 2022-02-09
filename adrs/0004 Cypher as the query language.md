# Cypher as the query language

## Status

Draft

## Decision

The language for queries defined in Markdown files is graph query language Cypher.

## Context

Users of the application need to be able to define their own queries, straight into their Markdown files. I need some kind abstraction - a query language - to make that work. I need to pick a language.

## Consequences

Cypher dictates graphs, so I'll need a graph database. TinkerGraph, the reference implementation of TinkerPop, seems to fit the bill quite nicely.

## Options considered

1. **Cypher**
2. JPQL
3. SQL
4. Some custom language

### Cypher

#### Pros

- Cypher is a platform independent graph query language. This fits in the model I envision.
- Cypher implementations are available for several graph databases, including the in-memory graph database TinkerGraph (see ADR0003).

#### Cons

- Cypher, although well documented, is not the best known query language. It's not as popular as SQL.
- In order to run Cypher queries I'll have to map the in-memory documents to a graph model and store them in a database. (But: creating nodes and edges is actually fairly straightforward and doesn't require things like table definitions or custom Java classes.) 

### JPQL

#### Pros

- JPQL is a "platform independent object-oriented query language". This fits in the model I envision.
- ANTLR grammars are readily available. Parsing queries is basically a no-brainer, including human-readable error messaging.

#### Cons

- Although JPQL is a standard, it's not as well-known as SQL.
- There are no libraries I could find that execute JPQL queries against in-memory models. It's really positioned as a tool in the ORM-ecosystem. I don't need nor want the "RM". So I'll have to implement a query execution engine myself.
- To define the object model (entities), the user will likely have to define custom classes. 

### SQL

#### Pros

- SQL is a well-known standard.

#### Cons

- SQL maps to tables and columns - relations - which is not the model I have in mind.
- SQL is much more feature-rich than JPQL and therefore more complex.

### Some custom language

#### Pros

- All the freedom in the world to come up with the best language.

#### Cons

- No familiarity of users with the language, requiring a lot of documentation.
- Coming up with a proper language requires lots of long and hard thinking, lots of experimenting and lots of work. Progress will be minimal.
