package nl.ulso.curator.query.builtin;

import nl.ulso.curator.Changelog;
import nl.ulso.curator.query.*;
import nl.ulso.curator.vault.*;

import jakarta.inject.Inject;
import java.util.*;

import static java.lang.System.lineSeparator;
import static nl.ulso.curator.Change.isPayloadType;

/**
 * Generates a table of contents from the current document.
 */
public final class TableOfContentsQuery
        implements Query
{
    @Inject
    TableOfContentsQuery()
    {
    }

    @Override
    public String name()
    {
        return "toc";
    }

    @Override
    public String description()
    {
        return "Outputs a table of contents for the current document.";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of("minimum-level", "minimum section level to include, defaults to 2",
                "maximum-level", "maximum section level to include, defaults to 6");
    }

    @Override
    public boolean isImpactedBy(Changelog changelog, QueryDefinition definition)
    {
        return changelog.changes().anyMatch(isPayloadType(Document.class).and(change ->
            change.as(Document.class).value().equals(definition.document())));
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        var configuration = definition.configuration();
        var minimumLevel = configuration.integer("minimum-level", 2);
        var maximumLevel = configuration.integer("maximum-level", 6);
        var tocBuilder = new TableOfContentsBuilder(minimumLevel, maximumLevel);
        definition.document().accept(tocBuilder);
        return () ->
        {
            var builder = new StringBuilder();
            tocBuilder.sections.forEach(section ->
                    {
                        var indentLevel = (section.level() - tocBuilder.minimumLevel) * 4;
                        builder.append(" ".repeat(indentLevel))
                                .append("- ")
                                .append(section.title())
                                .append(lineSeparator());
                    }
            );
            return builder.toString();
        };
    }

    private static class TableOfContentsBuilder
            extends BreadthFirstVaultVisitor
    {
        private final List<Section> sections = new ArrayList<>();
        private final int minimumLevel;
        private final int maximumLevel;

        public TableOfContentsBuilder(int minimumLevel, int maximumLevel)
        {
            this.minimumLevel = minimumLevel;
            this.maximumLevel = maximumLevel;
        }

        @Override
        public void visit(Section section)
        {
            var level = section.level();
            if (level >= minimumLevel && level <= maximumLevel)
            {
                sections.add(section);
            }
            if (level <= maximumLevel)
            {
                super.visit(section);
            }
        }
    }
}
