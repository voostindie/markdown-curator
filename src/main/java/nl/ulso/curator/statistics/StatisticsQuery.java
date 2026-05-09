package nl.ulso.curator.statistics;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.Changelog;
import nl.ulso.curator.query.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import static java.util.Collections.emptyMap;

@Singleton
final class StatisticsQuery
    implements Query
{
    private final Statistics statistics;
    private final QueryResultFactory queryResultFactory;

    @Inject
    StatisticsQuery(Statistics statistics, QueryResultFactory queryResultFactory)
    {
        this.statistics = statistics;
        this.queryResultFactory = queryResultFactory;
    }

    @Override
    public String name()
    {
        return "statistics";
    }

    @Override
    public String description()
    {
        return "Generates statistics on the vault in YAML format.";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return emptyMap();
    }

    @Override
    public boolean isImpactedBy(Changelog changelog, QueryDefinition definition)
    {
        return true;
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        var output = new StringWriter();
        var writer = new PrintWriter(output, true);
        writer.println("```yaml");
        statistics.logTo(writer);
        writer.println("```");
        return queryResultFactory.string(output.toString());
    }
}
