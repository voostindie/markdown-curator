# Configuration loaded at runtime

## Status

Adopted

## Decision

Configuration settings like the delay for the curator and the locale for messages can be (re)configured dynamically through the first YAML code block of a configuration document in the vault.

Change processors, queries, query result formatters and measurement trackers can be bundled in *extensions* that are loaded and unloaded at runtime, as configured in the configuration document.  Extensions are Dagger subcomponents, compiled into the application; the configuration selects and parameterizes them. Loading or unloading extensions always triggers a complete reset of the curator.

## Context

All change processors and queries used to be configured statically, through Dagger modules. Changing the configuration of a module - for example the folder to read teams from - required a rebuild and restart of the application.

Options considered:

- **Loading new code at runtime**, through separate class loaders. Rejected: class unloading is fragile, and Dagger generates its code at compile time.
- **Incrementally initializing new change processors**. Rejected: the system is fully event-based (see ADR 0007). It keeps no history of changes, so there is nothing to replay to a new processor. The same holds for unloading: consumers of payload types produced by an unloaded processor would keep stale data.
- **Reset on every configuration change**. Adopted: the `Reset` change already rebuilds all state in all change processors and reruns all queries. Configuration changes are rare, so the cost is acceptable.

## Consequences

- A reserved change processor, the `ExtensionLoader`, reads the configuration document. It runs before the `VaultInitializer`, stages new extensions, and publishes a `Reset` if the configuration changed. On every `Reset`, including startup, it reloads the configuration.
- The `ChangeProcessorOrchestrator` activates staged extensions during a `Reset`, after the reserved change processors have run. It verifies and orders all change processors again. If that fails, the new configuration is rejected as a whole, and the current extensions stay active. The same happens if extension queries clash with existing queries.
- The query catalog and statistics are no longer fixed. They are replaced as a whole, never changed in place, so concurrent readers - queries running in parallel, the HTTP server - always see a consistent set.
- Classes in an extension use the `@ExtensionScope` instead of `@Singleton`, and their bindings are qualified with `@ExtensionBinding`, because subcomponents inherit the multibindings of their parent.
- Extensions can depend on bindings of the curator, but not the other way around, and not on each other. They can only interact through payload types in the changelog.
- Two instances of the same extension are not supported: they would produce and consume the same payload types, and register queries with the same names.
