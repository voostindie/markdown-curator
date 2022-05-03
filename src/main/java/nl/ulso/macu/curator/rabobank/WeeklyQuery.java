package nl.ulso.macu.curator.rabobank;

import nl.ulso.macu.query.Query;
import nl.ulso.macu.query.QueryResult;
import nl.ulso.macu.vault.Document;
import nl.ulso.macu.vault.QueryBlock;

import java.util.*;
import java.util.regex.Pattern;

import static java.lang.System.lineSeparator;
import static java.time.format.TextStyle.FULL;
import static java.util.Locale.US;
import static java.util.regex.Pattern.compile;
import static nl.ulso.macu.query.QueryResult.failure;

class WeeklyQuery
        implements Query
{
    private static final Pattern DOCUMENT_NAME_PATTERN =
            compile("^(\\d{4}) Week (\\d{2})$");

    private final Journal journal;

    WeeklyQuery(Journal journal)
    {
        this.journal = journal;
    }

    @Override
    public String name()
    {
        return "weekly";
    }

    @Override
    public String description()
    {
        return "outputs an overview of activities in a certain week";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of("year", "Year of the weekly. Default: parsed from the document name.",
                "week", "Week of the weekly. Default: parsed from the document name.");
    }

    @Override
    public QueryResult run(QueryBlock queryBlock)
    {
        var year = queryBlock.configuration().integer("year", -1);
        var week = queryBlock.configuration().integer("week", -1);
        if (year == -1 || week == -1)
        {
            var matcher = DOCUMENT_NAME_PATTERN.matcher(queryBlock.document().name());
            if (matcher.matches())
            {
                year = selectInt(year, matcher.group(1));
                week = selectInt(week, matcher.group(2));
            }
        }
        if (year == -1 || week == -1)
        {
            return failure("Invalid date: year " + year + ", week " + week);
        }
        return new Weekly(journal.forWeek(year, week));
    }

    private int selectInt(int value, String stringValue)
    {
        if (value != -1)
        {
            return value;
        }
        try
        {
            return Integer.parseInt(stringValue);
        }
        catch (NumberFormatException e)
        {
            return -1;
        }
    }

    private static class Weekly
            implements QueryResult
    {
        private final Map<String, Map<Document, List<JournalEntry>>> entries;

        public Weekly(Map<String, Map<Document, List<JournalEntry>>> entries)
        {
            this.entries = entries;
        }

        @Override
        public boolean isSuccess()
        {
            return true;
        }

        @Override
        public String toMarkdown()
        {
            if (entries.isEmpty())
            {
                return "No entries found for this week";
            }
            var builder = new StringBuilder();
            for (String folder : Journal.FOLDERS_IN_ORDER)
            {
                var map = entries.get(folder);
                if (map == null)
                {
                    continue;
                }
                builder.append("## ")
                        .append(folder)
                        .append(lineSeparator())
                        .append(lineSeparator());
                var documents =
                        map.keySet().stream().sorted(Comparator.comparing(Document::name)).toList();
                for (Document document : documents)
                {
                    builder.append("- **")
                            .append(document.link())
                            .append("**")
                            .append(lineSeparator());
                    for (JournalEntry entry : map.get(document))
                    {
                        builder.append("  - *")
                                .append(entry.date().getDayOfWeek().getDisplayName(FULL, US))
                                .append("*: [[")
                                .append(entry.section().document().name())
                                .append("#")
                                .append(entry.section().anchor())
                                .append("|")
                                .append(entry.subject())
                                .append("]]")
                                .append(lineSeparator());
                    }
                }
                builder.append(lineSeparator());
            }
            return builder.toString().trim();
        }
    }
}
