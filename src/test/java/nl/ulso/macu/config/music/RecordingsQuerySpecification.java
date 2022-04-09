package nl.ulso.macu.config.music;

import nl.ulso.macu.query.QueryRunner;
import nl.ulso.macu.query.QuerySpecification;
import nl.ulso.macu.vault.Dictionary;

public class RecordingsQuerySpecification
        implements QuerySpecification
{
    @Override
    public String type()
    {
        return "recordings";
    }

    @Override
    public String description()
    {
        return "lists all recordings of a song";
    }

    @Override
    public QueryRunner configure(Dictionary configuration)
    {
        return null;
    }
}
