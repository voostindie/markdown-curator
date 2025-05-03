package nl.ulso.markdown_curator.project;

import nl.ulso.markdown_curator.vault.Document;

import java.util.Optional;

public final class Project
{
    private final Document document;
    private final AttributeValueResolverRegistry attributeValueResolverRegistry;

    Project(Document document, AttributeValueResolverRegistry attributeValueResolverRegistry)
    {
        this.document = document;
        this.attributeValueResolverRegistry = attributeValueResolverRegistry;
    }

    public Document document()
    {
        return document;
    }

    public String name()
    {
        return document.name();
    }

    public <T> Optional<T> attributeValue(Attribute<T> attribute)
    {
        for (var resolver : attributeValueResolverRegistry.resolversFor(attribute))
        {
            @SuppressWarnings("unchecked")
            Optional<T> value = ((AttributeValueResolver<T>) resolver).resolveValue(this);
            if (value.isPresent())
            {
                return value;
            }
        }
        return Optional.empty();
    }
}
