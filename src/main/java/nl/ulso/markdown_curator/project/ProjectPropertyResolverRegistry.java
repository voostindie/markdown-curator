package nl.ulso.markdown_curator.project;

import java.util.List;

/**
 * Keeps a registry of {@link ProjectPropertyResolver}s per {@link ProjectProperty}.
 */
public interface ProjectPropertyResolverRegistry
{
    List<ProjectPropertyResolver> resolversFor(ProjectProperty projectProperty);
}
