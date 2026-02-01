package nl.ulso.curator.addon.project;

import nl.ulso.curator.vault.Document;

import java.util.*;

public interface ProjectRepository
{
    Map<String, Project> projectsByName();

    Collection<Project> projects();

    Optional<Project> projectFor(Document document);
}
