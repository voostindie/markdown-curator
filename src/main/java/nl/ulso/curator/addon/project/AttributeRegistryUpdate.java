package nl.ulso.curator.addon.project;

import nl.ulso.curator.change.Change;

import static nl.ulso.curator.change.Change.update;

/// Object produced by the [AttributeRegistry] whenever it processed one or more changes.
///
/// The registry always produces either zero or one [AttributeRegistryUpdate] objects.
public record AttributeRegistryUpdate()
{
    static final Change<AttributeRegistryUpdate> REGISTRY_CHANGE =
        update(new AttributeRegistryUpdate(), AttributeRegistryUpdate.class);
}
