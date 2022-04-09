package nl.ulso.macu.config.music;

import nl.ulso.macu.query.QueryRunner;
import nl.ulso.macu.query.QuerySpecification;
import nl.ulso.macu.vault.Dictionary;

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
