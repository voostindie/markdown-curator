package nl.ulso.curator.change;

import java.util.*;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.List.copyOf;

final class DefaultChangelog
    implements Changelog
{
    static final Changelog EMPTY_CHANGELOG = new DefaultChangelog(emptyList());

    private final List<Change<?>> changes;

    DefaultChangelog(List<Change<?>> changes)
    {
        this.changes = copyOf(changes);
    }

    DefaultChangelog(Collection<Change<?>> changes)
    {
        this.changes = copyOf(changes);
    }

    DefaultChangelog(Change<?>... changes)
    {
        this.changes = List.of(changes);
    }

    @Override
    public boolean isEmpty()
    {
        return changes.isEmpty();
    }

    @Override
    public Changelog append(Changelog changelog)
    {
        if (changelog instanceof DefaultChangelog other)
        {
            if (changes.isEmpty())
            {
                return changelog;
            }
            if (other.changes.isEmpty())
            {
                return this;
            }
            List<Change<?>> merge = new ArrayList<>(this.changes.size() + other.changes.size());
            merge.addAll(this.changes);
            merge.addAll(other.changes);
            return new DefaultChangelog(merge);
        }
        throw new IllegalArgumentException(
            "Cannot merge changelog with unknown type: " + changelog.getClass());
    }

    @Override
    public Stream<Change<?>> changes()
    {
        return changes.stream();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Stream<Change<T>> changesFor(Class<T> payloadType)
    {
        return changes.stream()
            .filter(change -> change.payloadType().equals(payloadType))
            .map(change -> (Change<T>) change);
    }

    @Override
    public Changelog changelogFor(Set<Class<?>> payloadTypes)
    {
        return Changelog.changelogFor(changes.stream()
            .filter(change -> payloadTypes.contains(change.payloadType()))
            .toList());
    }
}
