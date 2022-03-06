package nl.ulso.obsidian.watcher.graph;

import java.util.Map;

public interface VertexDescriptor
{
    String name();
    String label();
    Map<String, Object> properties();
}
