package nl.ulso.markdown_curator.project;

import java.util.Optional;

public interface AttributeValueResolver<T>
{
    Attribute<T> attribute();

    Optional<T> resolveValue(Project project);

    int order();
}
