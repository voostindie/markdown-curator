package nl.ulso.curator.addon.project;

import nl.ulso.curator.changelog.Change;

import java.util.Collection;
import java.util.List;

import static nl.ulso.curator.changelog.Change.update;

/// Object produced by the [AttributeRegistry] whenever it processed one or more changes.
///
/// The registry always produces either zero or one [AttributeRegistryUpdate] objects.
public record AttributeRegistryUpdate()
{
    static final Collection<Change<?>> REGISTRY_CHANGE =
        List.of(update(new AttributeRegistryUpdate(), AttributeRegistryUpdate.class));

    @Override
    public String toString()
    {
        return "AttributeRegistry";
    }
}
