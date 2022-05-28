package nl.ulso.markdown_curator.query.builtin;

import nl.ulso.markdown_curator.query.*;
import nl.ulso.markdown_curator.vault.BreadthFirstVaultVisitor;
import nl.ulso.markdown_curator.vault.Section;

import java.util.*;

import static java.lang.System.lineSeparator;

/**
 * Generates a table of contents from the current document.
 */
public final class TableOfContentsQuery
        implements Query
{
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
    public QueryResult run(QueryDefinition definition)
    {
        var configuration = definition.configuration();
        var minimumLevel = configuration.integer("minimum-level", 2);
        var maximumLevel = configuration.integer("maximum-level", 6);
        var tocBuilder = new TableOfContentsBuilder(minimumLevel, maximumLevel);
        definition.document().accept(tocBuilder);
        return () -> {
            var builder = new StringBuilder();
            tocBuilder.sections.forEach(section ->
                    {
                        var indentLevel = (section.level() - tocBuilder.minimumLevel) * 2;
                        builder.append(" ".repeat(indentLevel))
                                .append("- [[#")
                                .append(section.anchor())
                                .append("]]")
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
