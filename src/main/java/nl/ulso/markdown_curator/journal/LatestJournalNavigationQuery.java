package nl.ulso.markdown_curator.journal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.markdown_curator.Changelog;
import nl.ulso.markdown_curator.query.*;

import java.util.Map;

import static nl.ulso.markdown_curator.Change.isCreate;
import static nl.ulso.markdown_curator.Change.isDelete;
import static nl.ulso.markdown_curator.Change.isObjectType;

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
            "postfix", "Markdown to postfix the link with"
        );
    }

    @Override
    public boolean isImpactedBy(Changelog changelog, QueryDefinition definition)
    {
        return journal.latest().map(Daily::date).map(latestDaily ->
                changelog.changes().anyMatch(isObjectType(Daily.class)
                    .and(isCreate().or(isDelete()))
                    .and(change ->
                        {
                            var documentDate = change.as(Daily.class).object().date();
                            return documentDate.isEqual(latestDaily) || // Creation of the latest
                                   documentDate.isAfter(latestDaily);   // Deletion of a newer
                        }
                    )
                )
            )
            .orElse(false);
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
