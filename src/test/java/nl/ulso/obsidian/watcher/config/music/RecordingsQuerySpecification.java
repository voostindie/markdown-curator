package nl.ulso.obsidian.watcher.config.music;

import nl.ulso.obsidian.watcher.query.QueryRunner;
import nl.ulso.obsidian.watcher.query.QuerySpecification;
import nl.ulso.obsidian.watcher.vault.Dictionary;

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
