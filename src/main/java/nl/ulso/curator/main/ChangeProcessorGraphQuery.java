package nl.ulso.curator.main;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.ChangeProcessor;
import nl.ulso.curator.change.Changelog;
import nl.ulso.curator.query.*;
import nl.ulso.dictionary.Dictionary;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.String.join;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

/// Generates a Mermaid diagram of the change processors configured for the vault and their
/// relationships. Mostly for debugging purposes.
///
/// The graph supports 4 types of edges:
///
/// 1. produce: from a processor to the payloads it produces
/// 2. consume: from a payload to the processors that consume them
/// 3. require: from a payload to the processors that require them (without consuming them)
/// 4. order: from one processor to the next processor in line.
///
/// Self-references are ignored. (For example, a processor that produces itself as a payload type in
/// order for another processor to require it.)
///
/// This implementation depends on the internal [DefaultChangeProcessorOrchestrator] because it
/// needs more than the set of available [ChangeProcessor]s: it needs their order too.
@Singleton
final class ChangeProcessorGraphQuery
    implements Query
{
    private static final String EDGES_PROPERTY = "edges";
    private static final String ORIENTATION_PROPERTY = "orientation";
    private static final String INDENT = "    ";

    private final DefaultChangeProcessorOrchestrator orchestrator;
    private final QueryResultFactory queryResultFactory;

    @Inject
    ChangeProcessorGraphQuery(
        DefaultChangeProcessorOrchestrator orchestrator, QueryResultFactory queryResultFactory)
    {
        this.orchestrator = orchestrator;
        this.queryResultFactory = queryResultFactory;
    }

    @Override
    public String name()
    {
        return "processorgraph";
    }

    @Override
    public String description()
    {
        return "Generates a Mermaid diagram of the internal change processor graph.";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of(
            EDGES_PROPERTY, "Set of edges to include: " +
                            join(", ", Edge.supportedEdges()) +
                            ". Defaults to '" + Edge.ORDER.toString().toLowerCase() + "'.",
            ORIENTATION_PROPERTY, "Orientation of the graph: " +
                                  join(", ", Orientation.supportedOrientations()) +
                                  ". Defaults to '" +
                                  Orientation.TOP_TO_BOTTOM.toString().toLowerCase() + "'."
        );
    }

    @Override
    public boolean isImpactedBy(Changelog changelog, QueryDefinition definition)
    {
        return false;
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        var orientation = resolveOrientation(definition.configuration());
        var edges = resolveEdges(definition.configuration());
        var builder = new StringBuilder();
        builder.append("```mermaid")
            .append(lineSeparator())
            .append("graph ")
            .append(orientation.mermaidText())
            .append(lineSeparator());
        for (Node node : createNodes(orchestrator.changeProcessors()))
        {
            var hasOutput = false;
            for (var edge : Edge.values())
            {
                if (edges.contains(edge))
                {
                    var output = edge.toMermaidEdges(node);
                    builder.append(output);
                    hasOutput = hasOutput || !output.isBlank();
                }
            }
            if (hasOutput)
            {
                builder
                    .append(INDENT)
                    .append(node.name())
                    .append("@{ shape: subproc }")
                    .append(lineSeparator());
            }
        }
        builder.append("```").append(lineSeparator());
        return queryResultFactory.string(builder.toString());
    }

    private Orientation resolveOrientation(Dictionary configuration)
    {
        var orientations = HashMap.<String, Orientation>newHashMap(Orientation.values().length);
        Arrays.stream(Orientation.values()).forEach(
            orientation -> orientations.put(orientation.mermaidText().toLowerCase(), orientation));
        var selection = configuration.string(
            ORIENTATION_PROPERTY,
            Orientation.LEFT_TO_RIGHT.mermaidText()
        ).toLowerCase();
        return orientations.getOrDefault(selection, Orientation.LEFT_TO_RIGHT);
    }

    private Set<Edge> resolveEdges(Dictionary configuration)
    {
        var edges = HashMap.<String, Edge>newHashMap(Edge.values().length);
        Arrays.stream(Edge.values()).forEach(edge -> edges.put(edge.name().toLowerCase(), edge));
        var set = configuration.listOfStrings(EDGES_PROPERTY).stream()
            .map(String::toLowerCase)
            .map(edges::get)
            .filter(Objects::nonNull)
            .collect(toSet());
        if (!set.isEmpty())
        {
            return set;
        }
        return Set.of(Edge.ORDER);
    }

    private List<Node> createNodes(List<ChangeProcessor> processors)
    {
        var nodes = processors.stream().map(Node::new).toList();
        for (var i = 0; i < processors.size() - 1; i++)
        {
            nodes.get(i).setNext(processors.get(i + 1));
        }
        return nodes;
    }

    /// Simple wrapper around ChangeProcessors to represent a node in the graph.
    private static final class Node
    {
        private final ChangeProcessor processor;
        private String next;

        Node(ChangeProcessor processor)
        {
            this.processor = processor;
        }

        void setNext(ChangeProcessor processor)
        {
            this.next = processor != null ? processor.name() : null;
        }

        String name()
        {
            return processor.name();
        }

        Stream<String> produces()
        {
            return toStream(processor.producedPayloadTypes());
        }

        Stream<String> consumes()
        {
            return toStream(processor.consumedPayloadTypes());
        }

        Stream<String> requires()
        {
            return toStream(processor.requiredPayloadTypes());
        }

        Stream<String> next()
        {
            return next != null ? Stream.of(next) : Stream.empty();
        }

        private Stream<String> toStream(Set<Class<?>> set)
        {
            return set.stream().map(Class::getSimpleName)
                .filter(name -> !name.contentEquals(name()));
        }
    }

    /// Graph orientations, as supported by Mermaid.
    private enum Orientation
    {
        TOP_TO_BOTTOM("TB"),
        BOTTOM_TO_TOP("BT"),
        LEFT_TO_RIGHT("LR"),
        RIGHT_TO_LEFT("RL");

        private final String mermaidText;

        Orientation(String mermaidText)
        {
            this.mermaidText = mermaidText;
        }

        String mermaidText()
        {
            return mermaidText;
        }

        static Set<String> supportedOrientations()
        {
            return Arrays.stream(Orientation.values())
                .map(Orientation::mermaidText)
                .map(String::toLowerCase)
                .collect(toSet());
        }
    }

    /// Direction of the edge between nodes. (In Mermaid the direction is one way. In case of a
    /// BACKWARD edge, we simply swap the start and end nodes.)
    private enum Direction
    {
        FORWARD,
        BACKWARD
    }

    private enum Edge
    {
        ORDER(" ==> ", Direction.FORWARD, Node::next),
        PRODUCE(" --> ", Direction.FORWARD, Node::produces),
        CONSUME(" --> ", Direction.BACKWARD, Node::consumes),
        REQUIRE(" -.-> ", Direction.BACKWARD, Node::requires);

        private final String mermaidText;
        private final Direction direction;
        private final Function<Node, Stream<String>> endNodeResolver;

        Edge(
            String mermaidText,
            Direction direction,
            Function<Node, Stream<String>> endNodeResolver)
        {
            this.mermaidText = mermaidText;
            this.direction = direction;
            this.endNodeResolver = endNodeResolver;
        }

        String toMermaidEdges(Node node)
        {
            var startNode = node.name();
            return endNodeResolver.apply(node)
                .map(endNode -> switch (direction)
                {
                    case FORWARD -> startNode + mermaidText + endNode;
                    case BACKWARD -> endNode + mermaidText + startNode;
                })
                .map(text -> INDENT + text + lineSeparator())
                .collect(joining());
        }

        static Set<String> supportedEdges()
        {
            return Arrays.stream(Edge.values())
                .map(Object::toString)
                .map(String::toLowerCase)
                .collect(toSet());
        }
    }
}
