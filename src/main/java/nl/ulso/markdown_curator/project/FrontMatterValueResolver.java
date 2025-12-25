package nl.ulso.markdown_curator.project;

import nl.ulso.markdown_curator.vault.*;

import java.time.LocalDate;
import java.util.Optional;

/// Resolves project properties from front matter; the resolver reads from the same property as it
/// is written to.
///
/// The type of the value in the front matter must be one:
///
/// - [String],
/// - [Document],
/// - [Integer], or
/// - [LocalDate]
///
/// Other types are not supported and throw an [UnsupportedOperationException].
public final class FrontMatterValueResolver
    implements ValueResolver
{
    private final ProjectProperty property;
    private final Vault vault;

    FrontMatterValueResolver(ProjectProperty property, Vault vault)
    {
        this.property = property;
        this.vault = vault;
    }

    @Override
    public ProjectProperty property()
    {
        return property;
    }

    @Override
    public Optional<?> from(Project project)
    {
        var frontMatter = project.document().frontMatter();
        var valueType = property.valueType();
        if (valueType.isAssignableFrom(String.class))
        {
            return fromString(frontMatter);
        }
        else if (valueType.isAssignableFrom(Document.class))
        {
            return fromDocument(frontMatter);
        }
        else if (valueType.isAssignableFrom(Integer.class))
        {
            return fromInteger(frontMatter);
        }
        else if (valueType.isAssignableFrom(LocalDate.class))
        {
            return fromLocalDate(frontMatter);
        }
        throw new UnsupportedOperationException(
            String.format("Unsupported front matter type: %s", valueType));
    }

    private Optional<?> fromString(Dictionary frontMatter)
    {
        return Optional.ofNullable(frontMatter.string(property.frontMatterProperty(), null));
    }

    private Optional<?> fromDocument(Dictionary frontMatter)
    {
        var result = frontMatter.string(property.frontMatterProperty(), null);
        if (result == null)
        {
            return Optional.empty();
        }
        if (result.startsWith("[[") && result.endsWith("]]"))
        {
            result = result.substring(2, result.length() - 2);
        }
        return vault.findDocument(result);
    }

    private Optional<?> fromLocalDate(Dictionary frontMatter)
    {
        return Optional.ofNullable(frontMatter.date(property.frontMatterProperty(), null));
    }

    private Optional<?> fromInteger(Dictionary frontMatter)
    {
        var result = frontMatter.integer(property.frontMatterProperty(), Integer.MIN_VALUE);
        if (result == Integer.MIN_VALUE)
        {
            return Optional.empty();
        }
        return Optional.of(result);
    }

    @Override
    public int order()
    {
        return Integer.MAX_VALUE;
    }
}
