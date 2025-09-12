package nl.ulso.markdown_curator.project;

import nl.ulso.markdown_curator.vault.*;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Resolves project properties from front matter; the resolver reads from the same property as
 * the property that the value is written to.
 */
public final class FrontMatterProjectPropertyResolver
        implements ProjectPropertyResolver
{
    private final ProjectProperty property;
    private final Vault vault;

    FrontMatterProjectPropertyResolver(ProjectProperty property, Vault vault)
    {
        this.property = property;
        this.vault = vault;
    }

    @Override
    public ProjectProperty projectProperty()
    {
        return property;
    }

    @Override
    public Optional<?> resolveValue(Project project)
    {
        var frontMatter = project.document().frontMatter();
        var valueType = property.valueType();
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

    private Optional<?> resolveStringOrDocument(Dictionary frontMatter)
    {
        var result = frontMatter.string(property.frontMatterProperty(), null);
        if (result != null)
        {
            var valueType = property.valueType();
            if (valueType.isAssignableFrom(Document.class))
            {
                if (result.startsWith("[[") && result.endsWith("]]"))
                {
                    result = result.substring(2, result.length() - 2);
                }
                return vault.findDocument(result);
            }
            else if (valueType.isAssignableFrom(String.class))
            {
                return Optional.of(result);
            }
        }
        return Optional.empty();
    }

    private Optional<?> resolveInteger(Dictionary frontMatter)
    {
        var result = frontMatter.integer(property.frontMatterProperty(), Integer.MIN_VALUE);
        if (result == Integer.MIN_VALUE)
        {
            return Optional.empty();
        }
        return Optional.of(result);
    }

    private Optional<?> resolveLocalDate(Dictionary frontMatter)
    {
        return Optional.ofNullable(frontMatter.date(property.frontMatterProperty(), null));
    }

    @Override
    public int order()
    {
        return Integer.MAX_VALUE;
    }
}
