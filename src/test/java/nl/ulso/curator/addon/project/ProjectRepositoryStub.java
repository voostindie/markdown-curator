package nl.ulso.curator.addon.project;

import nl.ulso.curator.vault.Document;

import java.util.*;

public class ProjectRepositoryStub
    implements ProjectRepository
{
    private final Map<String, Project> projects = new HashMap<>();

    public ProjectRepositoryStub withProject(Document document)
    {
        addProjectFor(document);
        return this;
    }

    public void addProjectFor(Document document)
    {
        projects.put(document.name(), new Project(document));
    }

    public void removeProjectFor(Document document)
    {
        projects.remove(document.name());
    }

    @Override
    public Map<String, Project> projectsByName()
    {
        return projects;
    }

    @Override
    public Collection<Project> projects()
    {
        return projects.values();
    }

    @Override
    public Optional<Project> projectFor(Document document)
    {
        return projectNamed(document.name());
    }

    @Override
    public Optional<Project> projectNamed(String name)
    {
        return Optional.ofNullable(projects.get(name));
    }
}
