package nl.ulso.markdown_curator;

/// Orchestrates the execution of all [ChangeProcessor]s in the system in the right order and only
/// when needed.
///
/// There can be many independent but related change processors in the system. At application
/// startup the orchestrator orders the processors in such a way that all requirements are
/// satisfied:
///
/// - Producers of certain object types before consumers of these object types.
/// - All consumed object types have at least one producer.
///
/// When a processor is executed from a [Changelog], it receives only changes of object types it can
/// consume. If there are none, the processor is not executed at all. When a processor produces
/// object types other than it advocates, that is considered a programming error and will throw an
/// [IllegalStateException].
///
/// In case there is a processor that consumes changes that are not produced by other processors,
/// the associated object type must be registered as an [ExternalChangeObjectType] in the system, in
/// the Dagger module:
///
/// ```java
///     @Provides @IntoSet @ExternalChangeObjectType
///     static Class<?> provideExternalChangeObjectType()
///     {
///         return SomeCustomType.class;
///     }
/// ```
public interface ChangeProcessorOrchestrator
{
    /// Runs all change processors in the system in the right order and only when needed based on
    /// the provided [Change].
    ///
    /// @return the full changelog of all changes that was built up by and applied to the change
    /// processors.
    Changelog runFor(Change<?> change);
}
