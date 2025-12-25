package nl.ulso.markdown_curator.project;

import java.util.List;

/// Keeps a registry of [ValueResolver]s per [ProjectProperty].
public interface ValueResolverRegistry
{
    /// @return All resolvers for the specified property, in order of preference.
    List<ValueResolver> resolversFor(ProjectProperty property);
}
