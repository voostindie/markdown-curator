package nl.ulso.curator.query;

import java.util.List;
import java.util.Map;

record TableResult(List<String> columns,
                   List<QueryResultFactory.Alignment> alignments,
                   List<Map<String, String>> rows)
    implements QueryResult
{
}
