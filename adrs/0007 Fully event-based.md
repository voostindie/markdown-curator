# Fully event-based

## Status

Adopted

## Decision

The system has one method of processing changes: as a list of sequential events. 

## Context

An earlier implementation of the curator supported two methods to process changes:

1. Full. On initialization the Vault event was used by many change processors to reset themselves, building up their internal data structures based on the contents of the vault.
2. Incremental. Whenever a change came in, that change was processed and the internal data structures were updated for the change.

This dual approach led to duplicate logic. The full update typically required a finder to go through (part of) the vault to find the relevant "changes".  Also, the full update required the change processor to be aware of lower-level contexts, like the vault.

The decision was to remove the full update and replace it with incremental updates, always. After implementing it in a branch, I concluded this works better than the full update. No more duplicate and sometimes low-level logic.

## Consequences

The Vault event was retrofitted:

- A special VaultInitializer picks up the Vault event triggered by the curator and produces a CREATE change for every folder and document in the vault. That way, every processor can "incrementally" process all documents in the vault on startup.
- Whenever a Vault event comes in, change processors can clean up their internal data structures, and should do nothing else. The VaultInitializer will have already pushed new changes to the changelog that each processor will have to process again, as if the system was booting up.
