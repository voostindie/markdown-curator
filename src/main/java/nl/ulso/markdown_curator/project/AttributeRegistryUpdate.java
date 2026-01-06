package nl.ulso.markdown_curator.project;

import nl.ulso.markdown_curator.Change;

import java.util.Collection;
import java.util.List;

import static nl.ulso.markdown_curator.Change.update;

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
