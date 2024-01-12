package nl.ulso.markdown_curator.journal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.markdown_curator.query.*;

import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
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
        return parseDateFrom(document).map(date ->
        {
            var title = Stream.of(
                            toLink(journal.dailyBefore(date), messages.journalPrevious()),
                            toLink(journal.dailyAfter(date), messages.journalNext()),
                            toLink(journal.weeklyFor(date), messages.journalUp()),
                            Optional.of(messages.journalDay(date)))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(joining(" "));
            return resultFactory.string("# " + title);
        }).orElseGet(() ->
                resultFactory.error("Document is not a daily journal!")
        );
    }
}
