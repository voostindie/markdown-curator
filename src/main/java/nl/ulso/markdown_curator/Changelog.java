package nl.ulso.markdown_curator;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

/// Log of changes consumed and produced by [ChangeProcessor]s.
///
/// The changelog acts like an event bus with multiple topics. Events on the bus are [Change]s, the
/// topics are the payload types of the [Change]. At the same time, the changelog is a sequential
/// log of what's happening in a single run of the system whenever a change is detected.
///
/// Starting with an initial change - typically a change to a file in the vault - all
/// [ChangeProcessor]s that consume vault changes are executed in order. In turn they can produce
/// other changes with other, domain-specific payload types, to be picked up by other
/// [ChangeProcessor]s later in the same run. At the end of the run, the full changelog provides a
/// complete view on what has happened.
///
/// @see ChangeProcessor
public interface Changelog
{
    static Changelog emptyChangelog()
    {
        return ChangelogImpl.EMPTY_CHANGELOG;
    }

    static Changelog changelogFor(Change<?>... changes)
    {
        return new ChangelogImpl(changes);
    }

    static Changelog changelogFor(Collection<Change<?>> changes)
    {
        return (changes.isEmpty()) ? emptyChangelog() : new ChangelogImpl(changes);
    }

    boolean isEmpty();

    Changelog append(Changelog changelog);

    Stream<Change<?>> changes();

    <T> Stream<Change<T>> changesFor(Class<T> payloadType);

    Changelog changelogFor(Set<Class<?>> payloadTypes);
}
