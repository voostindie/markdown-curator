package nl.ulso.markdown_curator;

import jakarta.inject.Singleton;
import nl.ulso.markdown_curator.vault.*;

import java.util.Set;

import static java.util.Collections.emptySet;

/// Processes changes in a [Changelog]
///
/// Change processors can turn changes happening to the vault into internal data structures, and/or
/// create new changes to be consumed by other processors.
///
/// Processors only receive changes of types they declare to consume ([#consumedObjectTypes()]) and
/// are not allowed to produce changes of types they do not declare to produce
/// ([#producedObjectTypes()]).
///
/// **Important**: change processors must be singletons (marked with [Singleton]).
///
/// The [ChangeProcessorOrchestrator] orchestrates the processing of changes by change processors,
/// making sure all processors in the system run in the correct order.
public interface ChangeProcessor
{
    /// Process a changelog containing only changes to object types that this processor consumes and
    /// returning only changes to object types that this processor produces.
    ///
    /// @param changelog changelog to process.
    /// @return changelog with changes.
    /// @see #consumedObjectTypes()
    /// @see #producedObjectTypes()
    Changelog run(Changelog changelog);

    /// Returns the set of object types that this model can consume from changelogs; this defaults
    /// to changes to objects from the vault. When processing a changelog, all object types that do
    /// not satisfy this set are filtered out. If the resulting changelog is empty, the model is not
    /// refreshed at all.
    ///
    /// At application startup all change processors are inspected on what they consume and ordered
    /// after the producers of these object types. If a required object type is missing, the
    /// application fails to start.
    default Set<Class<?>> consumedObjectTypes()
    {
        return Set.of(Vault.class, Document.class, Folder.class);
    }

    /// Returns the set of object types this change processor will produce from changelogs; this
    /// defaults to an empty set. When processing a changelog, any object type produced by this
    /// method not in this set is logged as an error.
    default Set<Class<?>> producedObjectTypes()
    {
        return emptySet();
    }
}
