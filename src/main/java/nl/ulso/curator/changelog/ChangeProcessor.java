package nl.ulso.curator.changelog;

import jakarta.inject.Singleton;

import java.util.Set;

import static java.util.Collections.emptySet;

/// Processes changes in a [Changelog]
///
/// Change processors can turn changes happening to the vault into internal data structures, and/or
/// create new changes to be consumed by other processors.
///
/// Processors only receive changes of types they declare to consume ([#consumedPayloadTypes()]) and
/// are not allowed to produce changes of types they do not declare to produce
/// ([#producedPayloadTypes()]).
///
/// **Important**: change processors must be singletons (marked with [Singleton]).
///
/// The change processors in the system are orchestrated such that all processors run in the correct
/// order: all producers of a payload type before all consumers of that payload type.
public interface ChangeProcessor
{
    /// Process a changelog containing only changes to payload types that this processor consumes
    /// and returning only changes to payload types that this processor produces.
    ///
    /// @param changelog changelog to process.
    /// @return changelog with changes.
    /// @see #consumedPayloadTypes()
    /// @see #producedPayloadTypes()
    Changelog run(Changelog changelog);

    /// Returns the set of payload types that this model can consume from changelogs.
    ///
    /// When processing a changelog, all payload types that do not satisfy this set are filtered
    /// out. If the resulting changelog is empty, the processor is not called at all.
    ///
    /// At application startup all change processors are inspected on what they consume and ordered
    /// after the producers of these payload types. If a required payload type is missing, the
    /// application fails to start.
    Set<Class<?>> consumedPayloadTypes();

    /// Returns the set of payload types this change processor will produce to changelogs; this
    /// defaults to an empty set. When processing a changelog, any payload type produced by this
    /// processor not in this set throws an exception.
    default Set<Class<?>> producedPayloadTypes()
    {
        return emptySet();
    }
}
