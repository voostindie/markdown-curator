package nl.ulso.markdown_curator.project;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Comparator.comparingInt;
import static java.util.HashMap.newHashMap;

@Singleton
final class DefaultAttributeValueResolverRegistry
        implements AttributeValueResolverRegistry
{
    private final Map<Attribute<?>, List<AttributeValueResolver<?>>> resolverMap;

    @Inject
    DefaultAttributeValueResolverRegistry(Set<AttributeValueResolver<?>> resolvers)
    {
        var temporaryMap = new HashMap<Attribute<?>, List<AttributeValueResolver<?>>>();
        for (AttributeValueResolver<?> resolver : resolvers)
        {
            temporaryMap.computeIfAbsent(resolver.attribute(),
                    k -> new LinkedList<>()).add(resolver);
        }

        for (List<AttributeValueResolver<?>> list : temporaryMap.values())
        {
            list.sort(comparingInt(AttributeValueResolver::order));
        }
        this.resolverMap = newHashMap(temporaryMap.size());
        temporaryMap.forEach((key, value) -> resolverMap.put(key, unmodifiableList(value)));
    }

    @Override
    public List<AttributeValueResolver<?>> resolversFor(Attribute<?> attribute)
    {
        return resolverMap.getOrDefault(attribute, emptyList());
    }
}
