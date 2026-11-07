# Dependencies between extensions

## Status

Adopted

## Decision

Extensions (see ADR 0009) can depend on each other through services and contributions, next to payload types:

- **Services** have exactly one provider. Extensions declare the services they provide and require in their factory. The curator creates providers before consumers and passes the services to consumers when they are created.
- **Contributions** follow the whiteboard pattern: any number of extensions contribute values to an extension point that another extension owns. Contributions are collected from the factories before any extension is created.

Extensions have a lifecycle: they are started when activated and stopped when replaced. Extensions that can't be loaded are dropped individually, together with the extensions that depend on them.

## Context

Splitting the project, journal and project journal modules into extensions requires the project journal to inject the `ProjectRepository` and the `Journal` that the other two extensions create. Dagger can't wire instances across subcomponents created at runtime.

The project module also owns a map of attribute definitions that other modules, like OmniFocus, add to. OmniFocus requires the `ProjectRepository`, so if contributions were services, project and OmniFocus would need each other first. Collecting contributions before creating anything breaks that cycle. That restricts contributions to plain values, which attribute definitions are.

Because every configuration change creates a complete new generation of extensions, services never change within a generation. That keeps the model far simpler than OSGi, where services come and go at any time, and it lets Dagger do the injection within each extension.

With dependencies, rejecting a configuration as a whole for a single mistake became too harsh: one typo would unload everything. Instead, the extensions that don't fit are dropped, and the reasons are reported through the `extensions` query.

The OmniFocus and new day modules started threads in their constructors. For extensions that is a leak: every configuration change would add another set of threads, and extensions that are created but not loaded would run anyway.

## Consequences

- The project, journal, project journal, OmniFocus and Alfred modules are extensions. Curators must include their extension modules and enable them in the configuration document. Their settings moved from Java to YAML; the OmniFocus include predicate became two settings: excluded prefixes and excluded names.
- Static code can't depend on extensions, because it outlives them. Code that depends on an extension must become an extension too.
- Infrastructure shared with static code - the `NewDayAlarm`, JavaScript for Automation and the OmniFocus database - stays in the curator. Extensions inject it from the curator; their modules include the module that provides it.
- A type that the curator binds can't be passed into an extension as a service, because Dagger reports a duplicate binding.
- Extension constructors must not acquire resources; `ExtensionLifecycle` does that instead.
