package nl.ulso.markdown_curator.project;

import nl.ulso.markdown_curator.vault.Document;

import java.util.*;

public interface ProjectRepository
{
    Map<String, Project> projectsByName();

    Collection<Project> projects();

    Optional<Project> projectFor(Document document);
}
