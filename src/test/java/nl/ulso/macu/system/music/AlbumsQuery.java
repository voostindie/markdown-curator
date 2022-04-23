package nl.ulso.macu.system.music;

import nl.ulso.macu.query.PreparedQuery;
import nl.ulso.macu.query.Query;
import nl.ulso.macu.vault.Dictionary;

public class AlbumsQuery
        implements Query
{
    @Override
    public String name()
    {
        return "albums";
    }

    @Override
    public String description()
    {
        return "lists all albums by an artist";
    }

    @Override
    public PreparedQuery prepare(Dictionary configuration)
    {
        return null;
    }
}
