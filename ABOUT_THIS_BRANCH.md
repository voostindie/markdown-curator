The goal of this branch is to implement one way of processing changes, instead of two.

Currently, there are two methods:

1. Full. On initialization the Vault event is used by many change processors to reset themselves, building up their internal data structures based on the contents of the vault.
2. Incremental. Whenever a change comes in, that change is processed and the internal data structures are updated for the change.

This dual approach leads to duplicate logic. The full update typically requires a finder to go through (part of) the vault to find the relevant "changes".

The idea of this branch is to remove the full update and replace it with incremental updates, always. The way to do that is to retrofit the Vault event:

- A special VaultInitializer picks up the Vault event triggered by the curator and produces a CREATE change for every folder and document in the vault. That way, every processor can "incrementally" process all documents in the vault on startup.
- Whenever a Vault event comes in, change processors can clean up their internal data structures, and nothing else. The VaultInitializer will have already pushed new changes to the changelog that each processor can process again, as if the system was booting up.

With this approach, it should be possible to remove all duplicate logic from the system.