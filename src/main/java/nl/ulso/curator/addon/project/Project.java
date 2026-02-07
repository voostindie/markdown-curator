package nl.ulso.curator.addon.project;

import nl.ulso.curator.vault.Document;

import java.util.Objects;

/// Represents a project; a simple wrapper around [Document]s.
///
/// **Important**: as it is not safe to store [Document]s in long-term program state, this applies
/// to [Project]s too.
///
/// @see Document
public record Project(Document document)
{
    public String name()
    {
        return document.name();
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        Project project = (Project) o;
        return document.name().equals(project.document.name());
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(document.name());
    }
}
