package nl.ulso.curator.addon.project;

import nl.ulso.curator.change.Change;

import static nl.ulso.curator.change.Change.update;

/// Object produced by the [ProjectAttributeRepository] whenever it processed one or more changes.
///
/// The repository always produces either zero or one [ProjectAttributeRepositoryUpdate] objects.
public record ProjectAttributeRepositoryUpdate()
{
    static final Change<ProjectAttributeRepositoryUpdate> REPOSITORY_UPDATE =
        update(new ProjectAttributeRepositoryUpdate(), ProjectAttributeRepositoryUpdate.class);

    @Override
    public String toString()
    {
        return ".";
    }
}
