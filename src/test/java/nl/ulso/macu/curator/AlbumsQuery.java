package nl.ulso.macu.curator;

import nl.ulso.macu.query.Query;
import nl.ulso.macu.query.QueryResult;
import nl.ulso.macu.vault.*;

import java.util.*;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;
import static nl.ulso.macu.query.QueryResult.list;

public class AlbumsQuery
        implements Query
{
    private final Vault vault;

    public AlbumsQuery(Vault vault)
    {
        this.vault = vault;
    }

    @Override
    public String name()
    {
        return "albums";
    }

    @Override
    public String description()
    {
        return "lists all albums by an artist, newest first";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of("artist", "Name of the artist. Defaults to document name.",
                "reverse", "Whether the list should be reversed. Defaults to false.");
    }

    @Override
    public QueryResult run(QueryBlock queryBlock)
    {
        var artist = queryBlock.configuration().string("artist", queryBlock.document().name());
        var reverse = queryBlock.configuration().bool("reverse", false);
        var finder = new AlbumFinder(artist, reverse);
        vault.accept(finder);
        return list(finder.albums.stream()
                .map(row -> "[[" + row.get("Title") + "]], " + row.get("Year")).toList());
    }

    private static class AlbumFinder
            extends BreadthFirstVaultVisitor
    {
        private static final Pattern YEAR_PATTERN = compile("(\\d{4})");

        private final String artist;
        private final boolean reverse;
        private final List<Map<String, String>> albums;

        AlbumFinder(String artist, boolean reverse)
        {
            this.artist = artist;
            this.reverse = reverse;
            this.albums = new ArrayList<>();
        }

        @Override
        public void visit(Folder folder)
        {
            if (folder.name().equals("albums"))
            {
                super.visit(folder);
            }
            albums.sort(new Comparator<>()
            {
                @Override
                public int compare(Map<String, String> o1, Map<String, String> o2)
                {
                    var y1 = year(o1);
                    var y2 = year(o2);
                    if (y1 == y2)
                    {
                        return o1.get("Title").compareTo(o2.get("Title"));
                    }
                    return Integer.compare(y2, y1);
                }

                private int year(Map<String, String> row)
                {
                    try
                    {
                        return Integer.parseInt(row.get("Year"));
                    }
                    catch (NumberFormatException e)
                    {
                        return 0;
                    }
                }
            });
            if (reverse)
            {
                Collections.reverse(albums);
            }
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
                    .anyMatch(link -> link.targetDocument().equals(artist));
            if (found)
            {
                String year;
                var matcher = YEAR_PATTERN.matcher(textBlock.content());
                if (matcher.find())
                {
                    year = matcher.group(1);
                }
                else
                {
                    year = "????";
                }
                albums.add(Map.of(
                        "Title", textBlock.document().name(),
                        "Year", year));
            }
        }
    }
}
