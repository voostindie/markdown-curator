package nl.ulso.markdown_curator.project;

import java.util.function.Function;

import static java.util.Objects.requireNonNull;

final class ProjectPropertyImpl
        implements ProjectProperty
{
    private final Class<?> valueType;
    private final String frontMatterProperty;
    private final Function<Object, Object> asFrontMatterFunction;

    ProjectPropertyImpl(
            Class<?> valueType, String frontMatterProperty,
            Function<Object, Object> asFrontMatterFunction)
    {
        this.valueType = valueType;
        this.frontMatterProperty = frontMatterProperty;
        this.asFrontMatterFunction = asFrontMatterFunction;
    }

    ProjectPropertyImpl(Class<?> valueType, String frontMatterProperty)
    {
        this(valueType, frontMatterProperty, Function.identity());
    }

    @Override
    public Class<?> valueType()
    {
        return valueType;
    }

    @Override
    public String frontMatterProperty()
    {
        return frontMatterProperty;
    }

    @Override
    public Object asFrontMatterValue(Object value)
    {
        requireNonNull(value);
        if (!valueType.isInstance(value))
        {
            throw new IllegalStateException(
                    String.format("Value '%s' must have type '%s'", value, valueType));
        }
        return asFrontMatterFunction.apply(value);
    }
}
