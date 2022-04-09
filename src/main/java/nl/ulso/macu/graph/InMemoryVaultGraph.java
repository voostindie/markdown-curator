package nl.ulso.macu.graph;

import nl.ulso.macu.vault.*;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.joining;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.addV;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.inV;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.unfold;

/**
 * Builds an in-memory graph from a vault, using Apache TinkerGraph.
 * <p/>
 * For every document in the vault a "document" vertex is created, and all links between documents
 * are represented with a "links_to" relationship. For every link to a non-existent document, a
 * dummy "discovered" vertex is created.
 * <p/>
 * The provided {@link Taxonomy} can further enhance the graph. Additional vertices and edges are
 * created where needed. In the end this results in a rich graph that can be queried at will.
 * <p/>
 * When building the graph, the implementation first goes through all documents in the vault to
 * create nodes, and then goes through the documents a second time to discover all the edges between
 * them. This makes sure that no vertices are unnecessarily seen as "discovered" initially, and that
 * edges are classified correctly.
 * <p/>
 * A hard requirement for this implementation is that document names are considered unique. Two
 * or more documents with the same name <strong>will</strong> lead to unpredictable behavior!
 */
public final class InMemoryVaultGraph
{
    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryVaultGraph.class);

    private static final String DOCUMENT = "document";
    private static final String DISCOVERED = "discovered";
    private static final String NAME = "name";
    private static final String LINKS_TO = "links_to";

    private final Vault vault;
    private final Taxonomy taxonomy;
    private final GraphTraversalSource g;

    public InMemoryVaultGraph(Vault vault, Taxonomy taxonomy)
    {
        this.vault = vault;
        this.taxonomy = taxonomy;
        var configuration = new BaseConfiguration();
        configuration.setProperty("gremlin.tinkergraph.vertexIdManager", "LONG");
        configuration.setProperty("gremlin.tinkergraph.edgeIdManager", "LONG");
        configuration.setProperty("gremlin.tinkergraph.vertexPropertyIdManager", "LONG");
        var graph = TinkerGraph.open(configuration);
        g = graph.traversal();
    }

    public void construct()
    {
        vault.accept(new VertexExtractor());
        vault.accept(new EdgeExtractor());
        if (LOGGER.isInfoEnabled())
        {
            LOGGER.info("Instantiated a graph for vault {} with {} vertices and {} edges",
                    vault.name(), g.V().count().next(), g.E().count().next());
        }
    }

    private final class VertexExtractor
            extends BreadthFirstVaultVisitor
    {
        @Override
        public void visit(Document document)
        {
            g.addV(DOCUMENT).property(NAME, document.name()).property("vaultPath",
                    currentVaultPath().stream().map(Folder::name).collect(joining("/"))).next();
            taxonomy.describe(currentVaultPath(), document).forEach(descriptor -> {
                var label = descriptor.label();
                if (label.equals(DOCUMENT) || label.equals(DISCOVERED))
                {
                    LOGGER.warn("Invalid taxonomy label {}. This one is reserved!", label);
                    return;
                }
                var v = g.addV(label);
                descriptor.properties().forEach(v::property);
                v.property(NAME, descriptor.name());
                v.next();
            });
        }
    }

    private final class EdgeExtractor
            extends BreadthFirstVaultVisitor
    {
        @Override
        public void visit(Document document)
        {
            // There are better ways to code this, with a single "g" call, but this is faster
            // than any such "one-liner" I could come up with.
            var from = g.V().has(DOCUMENT, NAME, document.name()).next().id();
            document.findInternalLinks().forEach(link -> {
                LOGGER.debug("Adding edge from {} to {}", document.name(), link.targetDocument());
                var edges = g.V(from).outE(LINKS_TO)
                        .where(inV().has(NAME, link.targetDocument())).fold()
                        .next();
                if (edges.isEmpty())
                {
                    var to = g.V().has(DOCUMENT, NAME, link.targetDocument())
                            .fold().coalesce(unfold(),
                                    addV(DISCOVERED).property(NAME, link.targetDocument())).as("to")
                            .next();
                    g.V(from).addE(LINKS_TO).to(to).property("count", 1).next();
                }
                else
                {
                    var edge = edges.get(0);
                    var count = (Integer) edge.property("count").value();
                    edge.property("count", count + 1);
                }
            });
        }
    }
}
