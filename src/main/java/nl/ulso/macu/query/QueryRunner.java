package nl.ulso.macu.query;

import nl.ulso.macu.vault.Vault;

public interface QueryRunner
{
    QueryResult run(Vault vault);
}
