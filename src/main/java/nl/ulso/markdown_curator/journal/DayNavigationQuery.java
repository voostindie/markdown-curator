package nl.ulso.markdown_curator.journal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.markdown_curator.Changelog;
import nl.ulso.markdown_curator.query.*;

import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static nl.ulso.markdown_curator.Change.isCreate;
import static nl.ulso.markdown_curator.Change.isDelete;
import static nl.ulso.markdown_curator.Change.isObjectType;
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
    public boolean isImpactedBy(Changelog changelog, QueryDefinition definition)
    {
        return isImpactByDaily(changelog, definition) || isImpactByWeekly(changelog, definition);
    }

    private boolean isImpactByDaily(Changelog changelog, QueryDefinition definition)
    {
        return parseDateFrom(definition.document())
            .map(documentDate ->
                changelog.changes().anyMatch(isObjectType(Daily.class)
                    .and(isCreate().or(isDelete()))
                    .and(change ->
                        {
                            var daily = change.objectAs(Daily.class).date();
                            var dailyBefore = journal.dailyBefore(daily).orElse(null);
                            var dailyAfter = journal.dailyAfter(daily).orElse(null);
                            return documentDate.equals(daily)
                                   || documentDate.equals(dailyBefore)
                                   || documentDate.equals(dailyAfter);
                        }
                    ))
            )
            .orElse(false);
    }

    private boolean isImpactByWeekly(Changelog changelog, QueryDefinition definition)
    {
        return parseDateFrom(definition.document())
            .map(documentDate ->
                changelog.changes().anyMatch(isObjectType(Weekly.class)
                    .and(isCreate().or(isDelete()))
                    .and(change ->
                        journal.computeWeeklyFor(documentDate).equals(change.objectAs(Weekly.class))
                    )
                )
            )
            .orElse(false);
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
                    Optional.of(messages.journalDay(date))
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(joining(" "));
            return resultFactory.string("# " + title);
        }).orElseGet(() ->
            resultFactory.error("Document is not a daily journal!")
        );
    }
}
