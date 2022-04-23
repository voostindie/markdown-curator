package nl.ulso.macu.system.music;

import nl.ulso.macu.query.PreparedQuery;
import nl.ulso.macu.query.Query;
import nl.ulso.macu.vault.Dictionary;

public class MembersQuery
        implements Query
{
    @Override
    public String name()
    {
        return "members";
    }

    @Override
    public String description()
    {
        return "lists all members of a band";
    }

    @Override
    public PreparedQuery prepare(Dictionary configuration)
    {
        return null;
    }
}
