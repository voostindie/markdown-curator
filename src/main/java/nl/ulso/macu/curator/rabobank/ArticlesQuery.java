package nl.ulso.macu.curator.rabobank;

import nl.ulso.macu.query.Query;
import nl.ulso.macu.query.QueryResult;
import nl.ulso.macu.vault.*;

import java.util.*;
import java.util.regex.Pattern;

import static java.util.Collections.emptyMap;
import static java.util.Comparator.comparing;
import static java.util.regex.Pattern.compile;
import static nl.ulso.macu.query.QueryResult.failure;
import static nl.ulso.macu.query.QueryResult.table;

class ArticlesQuery
        implements Query
{
    private final Vault vault;

    ArticlesQuery(Vault vault) {this.vault = vault;}

    @Override
    public String name()
    {
        return "articles";
    }

    @Override
    public String description()
    {
        return "outputs an overview of all articles";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return emptyMap();
    }

    @Override
    public QueryResult run(QueryBlock queryBlock)
    {
        return vault.folder("Articles").map(folder -> {
            var finder = new ArticleFinder();
            folder.accept(finder);
            var articles = finder.articles;
            articles.sort(comparing((Map<String, String> e) -> e.get("Date")).reversed());
            return table(List.of("Date", "Title", "Publication"), articles);
        }).orElse(failure("Couldn't find the folder 'Articles'"));
    }

    private static class ArticleFinder
            extends BreadthFirstVaultVisitor
    {
        private static final Pattern CHANGES_PATTERN =
                compile("^- \\[\\[(\\d{4}-\\d{2}-\\d{2})]]: (.*)$");

        private final List<Map<String, String>> articles = new ArrayList<>();

        @Override
        public void visit(Section section)
        {
            if (section.level() == 2 && section.title().contentEquals("Changes")
                    && section.fragments().size() > 0
                    && section.fragments().get(0) instanceof TextBlock textBlock)
            {
                var lines = new ArrayList<>(textBlock.lines());
                if (lines.isEmpty())
                {
                    return;
                }
                lines.sort(Comparator.reverseOrder());
                var mostRecent = lines.get(0);
                var matcher = CHANGES_PATTERN.matcher(mostRecent);
                if (matcher.matches())
                {
                    articles.add(Map.of(
                            "Date", matcher.group(1),
                            "Title", section.document().link(),
                            "Publication",
                            publicationLink(section.document().frontMatter()
                                    .string("publication", "Unpublished"))
                    ));
                }
            }
        }

        private String publicationLink(String property)
        {
            if (property.startsWith("https://"))
            {
                var name = "Link";
                if (property.contains("confluence"))
                {
                    name = "Confluence";
                }
                return "[" + name + "](" + property + ")";
            }
            return property;
        }
    }
}
