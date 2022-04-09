package nl.ulso.obsidian.watcher.query;

import java.util.List;
import java.util.Map;

public interface QueryResult
{
    boolean isValid();

    List<String> columns();

    List<Map<String, String>> rows();

    String errorMessage();
}
