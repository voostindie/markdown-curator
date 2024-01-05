package nl.ulso.markdown_curator.journal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.markdown_curator.query.*;

import static java.lang.System.lineSeparator;
import static nl.ulso.markdown_curator.journal.JournalBuilder.parseWeeklyFrom;

@Singleton
public class WeekNavigationQuery
        extends NavigationQueryTemplate
{
    @Inject
    protected WeekNavigationQuery(
            Journal journal, QueryResultFactory resultFactory, GeneralMessages messages)
    {
        super(journal, resultFactory, messages);
    }

    @Override
    public String name()
    {
        return "weeknav";
    }

    @Override
    public String description()
    {
        return "Generates calendar navigation in the weekly journal";
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        var document = definition.document();
        return parseWeeklyFrom(document).map(weekly ->
        {
            var builder = new StringBuilder();
            builder.append("# ");
            appendLinkTo(builder, journal.weeklyBefore(weekly), messages.journalPrevious());
            appendLinkTo(builder, journal.weeklyAfter(weekly), messages.journalNext());
            builder.append(messages.journalWeek(weekly.year(), weekly.week()))
                    .append(lineSeparator()).append(lineSeparator())
                    .append("## ");
            journal.dailiesForWeek(weekly).forEach(daily -> {
                var date = daily.date();
                var label = messages.journalWeekDay(journal.dayOfWeekNumberFor(date));
                appendLinkTo(builder, date.toString(), label);
            });
            return resultFactory.string(builder.toString().trim());

        }).orElseGet(() ->
                resultFactory.error("Document is not a weekly journal!")
        );
    }
}
