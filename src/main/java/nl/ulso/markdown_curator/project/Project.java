package nl.ulso.markdown_curator.project;

import nl.ulso.markdown_curator.vault.Document;

import java.util.Objects;

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
