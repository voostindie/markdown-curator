package nl.ulso.markdown_curator;

import nl.ulso.markdown_curator.query.*;
import nl.ulso.markdown_curator.vault.*;

import javax.inject.Inject;
import java.util.*;

import static java.util.Comparator.comparing;

public class MembersQuery
        implements Query
{
    private final Vault vault;
    private final QueryResultFactory resultFactory;

    @Inject
    public MembersQuery(Vault vault, QueryResultFactory resultFactory)
    {
        this.vault = vault;
        this.resultFactory = resultFactory;
    }

    @Override
    public String name()
    {
        return "members";
    }

    @Override
    public String description()
    {
        return "lists all members of a band";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of("artist", "Name of the band. Defaults to document name.");
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        var band = definition.configuration().string("artist", definition.document().name());
        var finder = new MemberFinder(band);
        vault.accept(finder);
        return resultFactory.unorderedList(finder.members.stream()
                .map(row -> "[[" + row.get("Name") + "]]").toList());
    }

    private static class MemberFinder
            extends BreadthFirstVaultVisitor
    {
        private final String band;
        private final List<Map<String, String>> members;

        public MemberFinder(String band)
        {
            this.band = band;
            this.members = new ArrayList<>();
        }

        @Override
        public void visit(Folder folder)
        {
            if (folder.name().equals("artists"))
            {
                super.visit(folder);
            }
            members.sort(comparing(e -> e.get("Name")));
        }

        @Override
        public void visit(Section section)
        {
            if (section.level() == 2
                && section.title().startsWith("About")
                && section.fragments().size() > 0)
            {
                section.fragments().get(0).accept(this);
            }
        }

        @Override
        public void visit(TextBlock textBlock)
        {
            var links = textBlock.findInternalLinks();
            var found = links.stream()
                    .anyMatch(link -> link.targetDocument().equals(band));
            if (found)
            {
                members.add(Map.of(
                        "Name", textBlock.document().name()));
            }
        }
    }
}
