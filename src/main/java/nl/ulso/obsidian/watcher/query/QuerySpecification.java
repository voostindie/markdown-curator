package nl.ulso.obsidian.watcher.query;

import nl.ulso.obsidian.watcher.vault.Dictionary;

public interface QuerySpecification
{
    String type();

    String description();

    QueryRunner configure(Dictionary configuration);
}
