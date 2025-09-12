package nl.ulso.markdown_curator.project;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Comparator.comparingInt;
import static java.util.HashMap.newHashMap;

@Singleton
final class ProjectPropertyResolverRegistryImpl
        implements ProjectPropertyResolverRegistry
{
    private final Map<ProjectProperty, List<ProjectPropertyResolver>> resolverMap;

    @Inject
    ProjectPropertyResolverRegistryImpl(Set<ProjectPropertyResolver> resolvers)
    {
        // Turn the list of resolvers into a map of lists, with each key being a unique project
        // property, and the resolvers in each list ordered on priority.
        var temporaryMap = new HashMap<ProjectProperty, List<ProjectPropertyResolver>>();
        for (ProjectPropertyResolver resolver : resolvers)
        {
            temporaryMap.computeIfAbsent(resolver.projectProperty(),
                    k -> new LinkedList<>()).add(resolver);
        }
        for (List<ProjectPropertyResolver> list : temporaryMap.values())
        {
            list.sort(comparingInt(ProjectPropertyResolver::order));
        }
        this.resolverMap = newHashMap(temporaryMap.size());
        temporaryMap.forEach((key, value) -> resolverMap.put(key, unmodifiableList(value)));
    }

    @Override
    public List<ProjectPropertyResolver> resolversFor(ProjectProperty projectProperty)
    {
        return resolverMap.getOrDefault(projectProperty, emptyList());
    }
}
