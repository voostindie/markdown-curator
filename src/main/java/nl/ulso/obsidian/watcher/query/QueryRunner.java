package nl.ulso.obsidian.watcher.query;

import nl.ulso.obsidian.watcher.vault.Vault;

public interface QueryRunner
{
    QueryResult run(Vault vault);
}
