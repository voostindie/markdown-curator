package nl.ulso.macu.query;

import java.util.List;
import java.util.Map;

public interface QueryResult
{
    static QueryResult failure(String errorMessage) {
        return new FailureResult(errorMessage);
    }

    static QueryResult table(List<String> columns, List<Map<String, String>> rows) {
        return new TableResult(columns, rows);
    }

    static QueryResult list(List<String> rows) {
        return new ListResult(rows);
    }

    boolean isSuccess();

    List<String> columns();

    List<Map<String, String>> rows();

    String errorMessage();

    String toString();
}
