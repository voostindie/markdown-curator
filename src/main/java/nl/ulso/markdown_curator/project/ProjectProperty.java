package nl.ulso.markdown_curator.project;

import java.util.function.Function;

/**
 * Defines a pluggable project property of a certain type.
 * <p/>
 * Project properties have several benefits:
 * <ul>
 *     <li>They can be used in queries efficiently; the system ensures that the value of each
 *     property is resolved exactly once per run.</li>
 *     <li>They can be extended; modules can add their own properties to the system.</li>
 *     <li>Their values can be resolved in more than one way through
 *     {@link ProjectPropertyResolver}s</li>s
 *     <li>They are automatically persisted in the front matter of the underlying project documents
 *     and persisted.</li>
 * </ul>
 */
public interface ProjectProperty
{
    /**
     * Built-in property for the last modification date of o project, a {@link java.time.LocalDate}
     */
    String LAST_MODIFIED = "last_modified";

    /**
     * Built-in property for the project lead, a {@link nl.ulso.markdown_curator.vault.Document}
     */
    String LEAD = "lead";

    /**
     * Built-in property for the priority of a project, a {@link Integer}.
     */
    String PRIORITY = "priority";

    /**
     * Built-in property for the status of a project, a {@link String}.
     */
    String STATUS = "status";

    static ProjectProperty newProperty(Class<?> valueType, String frontMatterProperty)
    {
        return new ProjectPropertyImpl(valueType, frontMatterProperty);
    }

    static ProjectProperty newProperty(
            Class<?> valueType, String frontMatterProperty,
            Function<Object, Object> asFrontMatterFunction)
    {
        return new ProjectPropertyImpl(valueType, frontMatterProperty, asFrontMatterFunction);
    }

    Class<?> valueType();

    String frontMatterProperty();

    Object asFrontMatterValue(Object value);
}
