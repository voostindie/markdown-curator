package nl.ulso.markdown_curator;

import nl.ulso.markdown_curator.query.*;
import nl.ulso.markdown_curator.vault.*;

import jakarta.inject.Inject;
import java.util.*;

import static java.util.Comparator.comparing;

public class RecordingsQuery
        implements Query
{
    private final Vault vault;
    private final QueryResultFactory resultFactory;

    @Inject
    public RecordingsQuery(Vault vault, QueryResultFactory resultFactory)
    {
        this.vault = vault;
        this.resultFactory = resultFactory;
    }

    @Override
    public String name()
    {
        return "recordings";
    }

    @Override
    public String description()
    {
        return "lists all recordings of a song";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of("song", "Name of the song. Defaults to document name.");
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        var song = definition.configuration().string("song", definition.document().name());
        var finder = new RecordingsFinder(song);
        vault.accept(finder);
        return resultFactory.unorderedList(finder.recordings.stream()
                .map(row ->
                        "Track " + row.get("Index") + " on [[" + row.get("Album") + "]]").toList());
    }

    private static class RecordingsFinder
            extends BreadthFirstVaultVisitor
    {
        private final String song;

        private final List<Map<String, String>> recordings;

        public RecordingsFinder(String song)
        {
            this.song = song;
            this.recordings = new ArrayList<>();
        }

        @Override
        public void visit(Folder folder)
        {
            if (folder.name().equals("albums"))
            {
                super.visit(folder);
            }
            recordings.sort(comparing(e -> e.get("Album")));
        }

        public void visit(Section section)
        {
            if (section.level() == 2
                && section.title().startsWith("Tracks")
                && section.fragments().size() > 0)
            {
                section.fragments().get(0).accept(this);
            }
        }

        @Override
        public void visit(TextBlock textBlock)
        {
            var tracks = textBlock.findInternalLinks().stream()
                    .filter(link -> link.targetDocument().equals(song)).toList();
            if (tracks.isEmpty())
            {
                return;
            }
            for (InternalLink track : tracks)
            {
                var link = track.toMarkdown();
                for (String line : textBlock.lines())
                {
                    if (line.endsWith(link))
                    {
                        var dot = line.indexOf('.');
                        if (dot != -1)
                        {
                            var index = line.substring(0, dot).trim();
                            recordings.add(Map.of(
                                    "Index", index,
                                    "Album", textBlock.document().name()
                            ));
                        }
                    }
                }
            }
        }
    }
}
