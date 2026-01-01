package nl.ulso.markdown_curator;

/// Orchestrates the refreshing of all [DataModel]s in the system in the right order and only when
/// needed.
///
/// There can be many independent but related data models in the system. At application startup the
/// orchestrator orders the data models in such a way that all requirements are satisfied:
///
/// - Producers of certain object types before consumers of these object types.
/// - Models before the models that depend on them.
/// - All consumed object types have at least one producer.
///
/// When a data model is refreshed from a [Changelog], it receives only changes of object types it
/// can consume. If there are none, the model is not refreshed. When a data model produces object
/// types other than it advocates, that is considered a programming error and will throw an
/// [IllegalStateException].
public interface DataModelOrchestrator
{
    /// Refreshes all data models in the system in the right order and only when needed based on the
    /// provided [Change].
    ///
    /// @return the full changelog of all changes that were applied to the data models.
    Changelog refreshAllDataModels(Change<?> change);
}
