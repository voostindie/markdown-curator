package nl.ulso.curator.change;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

/// Log of changes.
///
/// The changelog acts like an event bus with multiple topics. Events on the bus are [Change]s, the
/// topics are the payload types of the [Change]. At the same time, the changelog is a sequential
/// log of what's happening in a single run of the system whenever a change is detected.
public interface Changelog
{
    static Changelog emptyChangelog()
    {
        return DefaultChangelog.EMPTY_CHANGELOG;
    }

    static Changelog changelogFor(Change<?>... changes)
    {
        return new DefaultChangelog(changes);
    }

    static Changelog changelogFor(Collection<Change<?>> changes)
    {
        return (changes.isEmpty()) ? emptyChangelog() : new DefaultChangelog(changes);
    }

    boolean isEmpty();

    Changelog append(Changelog changelog);

    Stream<Change<?>> changes();

    <T> Stream<Change<T>> changesFor(Class<T> payloadType);

    Changelog changelogFor(Set<Class<?>> payloadTypes);
}
