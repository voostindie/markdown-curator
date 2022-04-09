package nl.ulso.macu.query;

import nl.ulso.macu.vault.Dictionary;

public interface QuerySpecification
{
    String type();

    String description();

    QueryRunner configure(Dictionary configuration);
}
