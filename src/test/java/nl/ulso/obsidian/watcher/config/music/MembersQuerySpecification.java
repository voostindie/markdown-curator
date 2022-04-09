package nl.ulso.obsidian.watcher.config.music;

import nl.ulso.obsidian.watcher.query.QueryRunner;
import nl.ulso.obsidian.watcher.query.QuerySpecification;
import nl.ulso.obsidian.watcher.vault.Dictionary;

public class MembersQuerySpecification
        implements QuerySpecification
{
    @Override
    public String type()
    {
        return "members";
    }

    @Override
    public String description()
    {
        return "lists all members of a band";
    }

    @Override
    public QueryRunner configure(Dictionary configuration)
    {
        return null;
    }
}
