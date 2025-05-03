package nl.ulso.markdown_curator.journal;

import nl.ulso.markdown_curator.query.*;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
            var title = Stream.of(
                            toLink(journal.weeklyBefore(weekly), messages.journalPrevious()),
                            toLink(journal.weeklyAfter(weekly), messages.journalNext()),
                            Optional.of(messages.journalWeek(weekly.year(), weekly.week())))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.joining(" "));
            var subtitle = journal.dailiesForWeek(weekly)
                    .map(daily ->
                    {
                        var date = daily.date();
                        var label = messages.journalWeekDay(journal.dayOfWeekNumberFor(date));
                        return toLink(Optional.of(date), label);
                    })
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.joining(" | "));
            return resultFactory.string("# " + title + lineSeparator() +
                                        lineSeparator() +
                                        "## " + subtitle);

        }).orElseGet(() ->
                resultFactory.error("Document is not a weekly journal!")
        );
    }
}
