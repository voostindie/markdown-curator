package nl.ulso.macu.config.music;

import nl.ulso.macu.query.QueryRunner;
import nl.ulso.macu.query.QuerySpecification;
import nl.ulso.macu.vault.Dictionary;

public class AlbumQuerySpecification
        implements QuerySpecification
{
    @Override
    public String type()
    {
        return "albums";
    }

    @Override
    public String description()
    {
        return "lists all albums by an artist";
    }

    @Override
    public QueryRunner configure(Dictionary configuration)
    {
        return null;
    }
}
