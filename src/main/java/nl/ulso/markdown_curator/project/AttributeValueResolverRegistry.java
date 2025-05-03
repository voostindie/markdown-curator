package nl.ulso.markdown_curator.project;

import java.util.List;

public interface AttributeValueResolverRegistry
{
    List<AttributeValueResolver<?>> resolversFor(Attribute<?> attribute);
}
