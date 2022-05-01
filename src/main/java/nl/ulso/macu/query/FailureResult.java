package nl.ulso.macu.query;

import java.util.List;
import java.util.Map;

import static java.lang.System.lineSeparator;
import static java.util.Collections.emptyList;

class FailureResult
        implements QueryResult
{
    private final String errorMessage;

    public FailureResult(String errorMessage)
    {
        this.errorMessage = errorMessage.trim();
    }

    @Override
    public boolean isSuccess()
    {
        return false;
    }

    @Override
    public List<String> columns()
    {
        return emptyList();
    }

    @Override
    public List<Map<String, String>> rows()
    {
        return emptyList();
    }

    @Override
    public String errorMessage()
    {
        return "The query failed to run. Reason:" +
                lineSeparator() +
                lineSeparator() +
                errorMessage;
    }

    @Override
    public String toMarkdown()
    {
        return errorMessage;
    }
}
