package nl.ulso.obsidian.watcher.graph;

import java.util.Map;

public interface DocumentDescriptor
{
    String name();
    String label();
    Map<String, Object> properties();
}
