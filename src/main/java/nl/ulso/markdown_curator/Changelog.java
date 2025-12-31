package nl.ulso.markdown_curator;

import nl.ulso.markdown_curator.vault.event.VaultChangedEvent;

import java.util.stream.Stream;

/// Log of changes applied by [DataModel]s in their [DataModel#vaultChanged(VaultChangedEvent)]
/// method. Other data models can use this log to better determine what happened.
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

    boolean isEmpty();

    Changelog append(Changelog changelog);

    Stream<Change<?>> changes();

    <T> Stream<Change<T>> changesFor(Class<T> objectType);
}
