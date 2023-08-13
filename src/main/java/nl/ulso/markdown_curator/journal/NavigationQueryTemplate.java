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

    protected void appendLinkTo(StringBuilder builder, Optional<?> documentName, String label)
    {
        documentName.ifPresent(name -> appendLinkInternal(builder, name, label));
    }

    protected void appendLinkTo(StringBuilder builder, String documentName, String label)
    {
        if (documentName != null)
        {
            appendLinkInternal(builder, documentName, label);
        }
    }

    private void appendLinkInternal(StringBuilder builder, Object documentName, String label)
    {
        builder.append("[[").append(documentName).append("|").append(label).append("]] ");
    }
}
