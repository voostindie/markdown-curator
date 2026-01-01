package nl.ulso.markdown_curator;

import java.util.*;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.List.copyOf;

final class ChangelogImpl
    implements Changelog
{
    static final Changelog EMPTY_CHANGELOG = new ChangelogImpl(emptyList());

    private final List<Change<?>> changes;

    ChangelogImpl(List<Change<?>> changes)
    {
        this.changes = copyOf(changes);
    }

    ChangelogImpl(Collection<Change<?>> changes)
    {
        this.changes = copyOf(changes);
    }

    ChangelogImpl(Change<?>... changes)
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
        if (changelog instanceof ChangelogImpl other)
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
            return new ChangelogImpl(merge);
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
    public <T> Stream<Change<T>> changesFor(Class<T> objectType)
    {
        return changes.stream()
            .filter(change -> change.objectType().equals(objectType))
            .map(change -> (Change<T>) change);
    }

    @Override
    public Changelog changelogFor(Set<Class<?>> objectTypes)
    {
        return Changelog.changelogFor(changes.stream()
            .filter(change -> objectTypes.contains(change.objectType()))
            .toList());
    }
}
