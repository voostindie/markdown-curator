package nl.ulso.markdown_curator.journal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.markdown_curator.query.*;

import static nl.ulso.markdown_curator.journal.JournalBuilder.parseDateFrom;

@Singleton
public class DayNavigationQuery
        extends NavigationQueryTemplate
{
    @Inject
    protected DayNavigationQuery(
            Journal journal, QueryResultFactory resultFactory, GeneralMessages messages)
    {
        super(journal, resultFactory, messages);
    }

    @Override
    public String name()
    {
        return "daynav";
    }

    @Override
    public String description()
    {
        return "Generates calendar navigation in the daily journal";
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        var document = definition.document();
        return parseDateFrom(document).map((date) ->
        {
            var builder = new StringBuilder();
            builder.append("# ");
            appendLinkTo(builder, journal.dailyBefore(date), messages.journalPrevious());
            appendLinkTo(builder, journal.dailyAfter(date), messages.journalNext());
            appendLinkTo(builder, journal.weeklyFor(date), messages.journalUp());
            builder.append(messages.journalDay(date));
            return resultFactory.string(builder.toString());
        }).orElseGet(() ->
                resultFactory.error("Document is not a daily journal!")
        );
    }
}
