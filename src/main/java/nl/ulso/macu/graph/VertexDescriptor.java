package nl.ulso.macu.graph;

import java.util.Map;

public interface VertexDescriptor
{
    String name();
    String label();
    Map<String, Object> properties();
}
