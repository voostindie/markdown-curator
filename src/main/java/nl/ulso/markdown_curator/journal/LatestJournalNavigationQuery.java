package nl.ulso.markdown_curator.journal;

import nl.ulso.markdown_curator.query.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

@Singleton
public class LatestJournalNavigationQuery
        extends NavigationQueryTemplate
{
    @Inject
    protected LatestJournalNavigationQuery(
            Journal journal, QueryResultFactory resultFactory, GeneralMessages messages)
    {
        super(journal, resultFactory, messages);
    }

    @Override
    public String name()
    {
        return "latestnav";
    }

    @Override
    public String description()
    {
        return "Generates a link to the latest journal entry";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of("prefix", "Markdown to prefix the link with",
                "postfix", "Markdown to postfix the link with");
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        var prefix = definition.configuration().string("prefix", "");
        var postfix = definition.configuration().string("postfix", "");
        return journal.latest()
                .map(daily -> resultFactory.string(
                        prefix +
                        "[[" + daily.date() + "|" + messages.journalLatest() + "]]" +
                        postfix)
                )
                .orElse(resultFactory.empty());
    }
}
