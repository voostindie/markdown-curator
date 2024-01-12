package nl.ulso.markdown_curator.journal;

import nl.ulso.markdown_curator.query.*;

import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyMap;

abstract class NavigationQueryTemplate
        implements Query
{
    protected final Journal journal;
    protected final QueryResultFactory resultFactory;
    protected final GeneralMessages messages;

    protected NavigationQueryTemplate(
            Journal journal, QueryResultFactory resultFactory, GeneralMessages messages)
    {
        this.journal = journal;
        this.resultFactory = resultFactory;
        this.messages = messages;
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return emptyMap();
    }

    protected Optional<String> toLink(Optional<?> documentName, String label)
    {
        return documentName.map(name -> "[[" + name + "|" + label + "]]");
    }
}
