package nl.ulso.markdown_curator.journal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.markdown_curator.Changelog;
import nl.ulso.markdown_curator.query.*;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.System.lineSeparator;
import static nl.ulso.markdown_curator.Change.isCreate;
import static nl.ulso.markdown_curator.Change.isDelete;
import static nl.ulso.markdown_curator.Change.isObjectType;
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
    public boolean isImpactedBy(Changelog changelog, QueryDefinition definition)
    {
        return isImpactByDaily(changelog, definition) || isImpactByWeekly(changelog, definition);
    }

    private boolean isImpactByDaily(Changelog changelog, QueryDefinition definition)
    {
        return parseWeeklyFrom(definition.document())
            .map(documentWeekly ->
                changelog.changes().anyMatch(isObjectType(Daily.class)
                    .and(isCreate().or(isDelete()))
                    .and(change ->
                        journal.weeklyFor(change.objectAs(Daily.class).date())
                            .map(documentWeekly::equals)
                            .orElse(false)
                    ))
            )
            .orElse(false);
    }

    private boolean isImpactByWeekly(Changelog changelog, QueryDefinition definition)
    {
        return parseWeeklyFrom(definition.document())
            .map(documentWeekly ->
                changelog.changes().anyMatch(isObjectType(Weekly.class)
                    .and(isCreate().or(isDelete()))
                    .and(change -> {
                            var weekly = change.objectAs(Weekly.class);
                            var weeklyBefore = journal.weeklyBefore(weekly).orElse(null);
                            var weeklyAfter = journal.weeklyAfter(weekly).orElse(null);
                            return documentWeekly.equals(weekly) ||
                                   documentWeekly.equals(weeklyBefore) ||
                                   documentWeekly.equals(weeklyAfter);
                        }
                    )
                )
            )
            .orElse(false);
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
                    Optional.of(messages.journalWeek(weekly.year(), weekly.week()))
                )
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
