package nl.ulso.markdown_curator.project;

import nl.ulso.markdown_curator.vault.*;

import java.time.LocalDate;
import java.util.Optional;

public final class FrontMatterAttributeValueResolver<T>
        implements AttributeValueResolver<T>
{
    private final Attribute<T> attribute;
    private final String propertyName;
    private final Vault vault;

    FrontMatterAttributeValueResolver(Attribute<T> attribute, String propertyName, Vault vault)
    {
        this.attribute = attribute;
        this.propertyName = propertyName;
        this.vault = vault;
    }

    @Override
    public Attribute<T> attribute()
    {
        return attribute;
    }

    @Override
    public Optional<T> resolveValue(Project project)
    {
        var frontMatter = project.document().frontMatter();
        var valueType = attribute.valueType();
        if (valueType.isAssignableFrom(LocalDate.class))
        {
            return resolveLocalDate(frontMatter);
        }
        else if (valueType.isAssignableFrom(Integer.class))
        {
            return resolveInteger(frontMatter);
        }
        else
        {
            return resolveStringOrDocument(frontMatter);
        }
    }

    private Optional<T> resolveStringOrDocument(Dictionary frontMatter)
    {
        var result = frontMatter.string(propertyName, null);
        if (result != null)
        {
            var valueType = attribute.valueType();
            if (valueType.isAssignableFrom(Document.class))
            {
                if (result.startsWith("[[") && result.endsWith("]]"))
                {
                    result = result.substring(2, result.length() - 2);
                }
                @SuppressWarnings("unchecked")
                var document = (Optional<T>) vault.findDocument(result);
                return document;
            }
            else if (valueType.isAssignableFrom(String.class))
            {
                @SuppressWarnings("unchecked")
                var string = (T) result;
                return Optional.of(string);
            }
        }
        return Optional.empty();
    }

    private Optional<T> resolveInteger(Dictionary frontMatter)
    {
        var result = frontMatter.integer(propertyName, Integer.MIN_VALUE);
        if (result == Integer.MIN_VALUE)
        {
            return Optional.empty();
        }
        @SuppressWarnings("unchecked")
        var integer = (T) Integer.valueOf(result);
        return Optional.of(integer);
    }

    private Optional<T> resolveLocalDate(Dictionary frontMatter)
    {
        var result = frontMatter.date(propertyName, null);
        if (result == null)
        {
            return Optional.empty();
        }
        @SuppressWarnings("unchecked")
        var date = (T) result;
        return Optional.of(date);
    }

    @Override
    public int order()
    {
        return Integer.MAX_VALUE;
    }
}
