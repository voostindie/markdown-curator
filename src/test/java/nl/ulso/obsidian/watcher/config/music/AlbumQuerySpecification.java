package nl.ulso.obsidian.watcher.config.music;

import nl.ulso.obsidian.watcher.query.QueryRunner;
import nl.ulso.obsidian.watcher.query.QuerySpecification;
import nl.ulso.obsidian.watcher.vault.Dictionary;

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
