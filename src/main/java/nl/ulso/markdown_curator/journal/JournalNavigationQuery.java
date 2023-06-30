package nl.ulso.markdown_curator.journal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.markdown_curator.query.*;
import nl.ulso.markdown_curator.vault.LocalDates;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

import static java.util.Collections.emptyMap;

@Singleton
public class JournalNavigationQuery
        implements Query
{
    private final Journal model;
    private final QueryResultFactory resultFactory;
    private final DateTimeFormatter formatter;

    @Inject
    JournalNavigationQuery(Journal model, QueryResultFactory resultFactory, Locale locale)
    {
        this.model = model;
        this.resultFactory = resultFactory;
        this.formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale);
    }

    @Override
    public String name()
    {
        return "navigator";
    }

    @Override
    public String description()
    {
        return "Generates calendar navigation in the daily journal";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return emptyMap();
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        var date = LocalDates.parseDateOrNull(definition.document().name());
        if (date == null)
        {
            return resultFactory.error("Document is not a daily journal!");
        }
        var result = new StringBuilder();
        result.append("# ");
        appendNavigator(result, model.entryBefore(date), "⬅️");
        appendNavigator(result, model.entryAfter(date), "➡️");
        result.append(date.format(formatter));
        return resultFactory.string(result.toString());
    }

    private void appendNavigator(
            StringBuilder builder, Optional<LocalDate> optionalDate, String label)
    {
        optionalDate.ifPresent(date ->
                builder.append("[[").append(date).append("|").append(label).append("]] ")
        );
    }
}
