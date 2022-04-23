package nl.ulso.macu.query;

import nl.ulso.macu.vault.Dictionary;

public interface Query
{
    String name();

    String description();

    PreparedQuery prepare(Dictionary configuration);
}
