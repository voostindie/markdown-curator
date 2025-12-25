package nl.ulso.markdown_curator.project;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparingInt;
import static java.util.HashMap.newHashMap;
import static java.util.List.copyOf;

@Singleton
final class ValueResolverRegistryImpl
    implements ValueResolverRegistry
{
    private final Map<ProjectProperty, List<ValueResolver>> resolvers;

    @Inject
    ValueResolverRegistryImpl(Set<ValueResolver> resolvers)
    {
        // Turn the set of resolvers into a map of lists, with each key being a unique project
        // property, and the resolvers in each list ordered on priority.
        var map = new HashMap<ProjectProperty, List<ValueResolver>>();
        for (var resolver : resolvers)
        {
            map.computeIfAbsent(resolver.property(), k -> new LinkedList<>())
                .add(resolver);
        }
        for (var list : map.values())
        {
            list.sort(comparingInt(ValueResolver::order));
        }
        this.resolvers = newHashMap(map.size());
        map.forEach((property, list) ->
            this.resolvers.put(property, copyOf(list))
        );
    }

    @Override
    public List<ValueResolver> resolversFor(ProjectProperty property)
    {
        return resolvers.getOrDefault(property, emptyList());
    }
}
