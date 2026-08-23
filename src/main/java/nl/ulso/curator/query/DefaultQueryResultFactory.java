package nl.ulso.curator.query;

import jakarta.inject.Inject;

import java.util.*;

import static java.util.Collections.unmodifiableList;

class DefaultQueryResultFactory
    implements QueryResultFactory
{
    private final GeneralMessages messages;

    public DefaultQueryResultFactory()
    {
        this(new ResourceBundleGeneralMessages());
    }

    @Inject
    public DefaultQueryResultFactory(GeneralMessages messages)
    {
        this.messages = messages;
    }

    @Override
    public QueryResult string(String output)
    {
        if (output.isBlank())
        {
            return empty();
        }
        return new StringResult(output);
    }

    @Override
    public QueryResult empty()
    {
        return new EmptyResult(messages.noResults());
    }

    @Override
    public QueryResult error(String errorMessage)
    {
        return new ErrorResult(errorMessage);
    }

    @Override
    public QueryResult table(List<String> columns, List<Map<String, String>> rows)
    {
        return table(
            columns,
            Collections.nCopies(columns.size(), Alignment.LEFT),
            rows
        );
    }

    @Override
    public QueryResult table(
        List<String> columns, List<Alignment> alignments,
        List<Map<String, String>> rows)
    {
        if (rows.isEmpty())
        {
            return empty();
        }
        return new TableResult(
            unmodifiableList(columns),
            unmodifiableList(alignments),
            unmodifiableList(rows)
        );
    }

    @Override
    public QueryResult unorderedList(List<String> rows)
    {
        if (rows.isEmpty())
        {
            return empty();
        }
        return new UnorderedListResult(unmodifiableList(rows));
    }
}
