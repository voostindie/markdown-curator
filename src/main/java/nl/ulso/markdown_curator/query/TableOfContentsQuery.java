package nl.ulso.markdown_curator.query;

import nl.ulso.markdown_curator.vault.*;

import java.util.*;

import static java.lang.System.lineSeparator;

public class TableOfContentsQuery
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
        return "outputs a table of contents for the current document";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of("minimum-level", "minimum section level to include, defaults to 2",
                "maximum-level", "maximum section level to include, defaults to 6");
    }

    @Override
    public QueryResult run(QueryBlock queryBlock)
    {
        var minimumLevel = queryBlock.configuration().integer("minimum-level", 2);
        var maximumLevel = queryBlock.configuration().integer("maximum-level", 2);
        var tocBuilder = new TableOfContentsBuilder(minimumLevel, maximumLevel);
        queryBlock.document().accept(tocBuilder);
        return () -> {
            var builder = new StringBuilder();
            tocBuilder.sections.forEach(section ->
                    builder.append(" ".repeat((section.level() - tocBuilder.minimumLevel) * 2))
                            .append("- [[#")
                            .append(section.anchor())
                            .append("]]")
                            .append(lineSeparator())
            );
            return builder.toString().trim();
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
            if (section.level() >= minimumLevel)
            {
                sections.add(section);
            }
            if (section.level() <= maximumLevel)
            {
                super.visit(section);
            }
        }
    }
}
