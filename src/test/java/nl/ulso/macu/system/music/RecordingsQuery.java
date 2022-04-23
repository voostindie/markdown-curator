package nl.ulso.macu.system.music;

import nl.ulso.macu.query.PreparedQuery;
import nl.ulso.macu.query.Query;
import nl.ulso.macu.vault.Dictionary;

public class RecordingsQuery
        implements Query
{
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
    public PreparedQuery prepare(Dictionary configuration)
    {
        return null;
    }
}
