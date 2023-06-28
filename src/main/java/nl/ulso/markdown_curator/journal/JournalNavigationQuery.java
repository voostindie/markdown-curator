package nl.ulso.markdown_curator.journal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.markdown_curator.query.*;
import nl.ulso.markdown_curator.vault.LocalDates;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Map;

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
        var before = model.entryBefore(date);
        var after = model.entryAfter(date);
        var result = new StringBuilder();
        result.append("# ");
        before.ifPresent(d ->
        {
            result.append("[[");
            result.append(d);
            result.append("|⬅️]] ");
        });
        result.append(date.format(formatter));
        after.ifPresent(d ->
        {
            result.append(" [[");
            result.append(d);
            result.append("|➡️]]");
        });
        return resultFactory.string(result.toString());
    }
}
