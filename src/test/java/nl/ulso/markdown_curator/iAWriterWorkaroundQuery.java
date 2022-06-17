package nl.ulso.markdown_curator;

import nl.ulso.markdown_curator.query.*;
import nl.ulso.markdown_curator.vault.Vault;

import java.util.List;
import java.util.Map;

public class iAWriterWorkaroundQuery
        implements Query
{
    private final Vault vault;

    public iAWriterWorkaroundQuery(Vault vault)
    {
        this.vault = vault;
    }

    @Override
    public String name()
    {
        return "ia";
    }

    @Override
    public String description()
    {
        return "lists all albums by an artist in a table, newest first";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of("artist", "Name of the artist. Defaults to document name.");
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        var artist = definition.configuration().string("artist", definition.document().name());
        var finder = new AlbumsQuery.AlbumFinder(artist, false);
        vault.accept(finder);
        return QueryResult.table(List.of("Title", "Year"), finder.albums());
    }
}
