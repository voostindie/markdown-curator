# All processing done in memory

## Status

Adopted

## Decision

Everything this application does except for producing output is done in memory. Nothing is persisted to disk or stored elsewhere using some external service (like a database). Only output is stored to disk, where the user wants it.

## Context

This program processes Markdown files on disk. It extracts information from plaintext files, adds meaning to it, runs queries on top of it, and generates reports. To run these queries, it logically needs something to run these queries against. The decision is to have this "database" only in memory.

## Consequences

The available memory limits the maximum size of the vaults this application can process. The larger the vault, the more memory is required.

Because vaults contain human-written notes only (the human being me, typically), it will take many years before the amount of memory required outgrows the amount of memory available. If that happens to me, by then I'll just buy a bigger machine.

## Options considered

1. **Everything in memory**
2. Caches on disk
3. Some kind of database

### Everything in memory

#### Pros

- No mappings; no indirections to access data.
- No need to keep stuff in sync.

#### Cons

- More data means more memory. There's no way to keep the memory usage constant.

### Caches on disk

#### Pros

- Faster startup (maybe): restore the cache, and the program is ready to go. But this is premature optimization. In my first tests I was able to load all documents in my biggest vault (52 MB, maintained since 2017) in less than a quarter of a second.

#### Cons

- "There are only two hard things in Computer Science: cache invalidation and naming things." (Phil Karlton). Amen to that. 

### Some kind of database

#### Pros

- Memory usage can be much more controlled and made constant.
- Query language could be exposed to the user to easily construct queries.

#### Cons

- Keeping the database in sync with the files on disk is more work.
- Mapping in-memory data to the database and back again is needed. No matter the type of database, it's not automatic.
- Another dependency to manage. Maybe it can be bundled with the application (e.g. H2, Hazelcast), but it's still there.
