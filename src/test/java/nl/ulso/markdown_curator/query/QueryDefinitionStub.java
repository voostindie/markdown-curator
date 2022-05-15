package nl.ulso.markdown_curator.query;

import nl.ulso.markdown_curator.vault.Dictionary;
import nl.ulso.markdown_curator.vault.Document;

import java.util.HashMap;
import java.util.Map;

public class QueryDefinitionStub implements QueryDefinition
{
    private final Query query;
    private final Document document;
    private final Map<String, Object> configuration;

    public QueryDefinitionStub(Query query, Document document)
    {
        this.query = query;
        this.document = document;
        configuration = new HashMap<>();
    }

    public QueryDefinitionStub withConfiguration(String name, Object value)
    {
        configuration.put(name, value);
        return this;
    }

    @Override
    public String name()
    {
        return query.name();
    }

    @Override
    public Dictionary configuration()
    {
        return Dictionary.mapDictionary(configuration);
    }

    @Override
    public Document document()
    {
        return document;
    }

    @Override
    public String result()
    {
        return "";
    }
}
