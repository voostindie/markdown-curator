package nl.ulso.curator.addon.journal;

import jakarta.inject.Inject;
import nl.ulso.curator.query.QueryDefinition;
import nl.ulso.curator.query.QueryResultFactory;
import nl.ulso.curator.vault.Vault;

import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Stream;

import static nl.ulso.curator.addon.journal.Weekly.parseWeeklyFrom;

public class WeeklyQuery
    extends PeriodQuery
{
    @Inject
    WeeklyQuery(Journal journal, Vault vault, JournalSettings settings, QueryResultFactory resultFactory)
    {
        super(journal, vault, settings, resultFactory);
    }

    @Override
    public String name()
    {
        return "weekly";
    }

    @Override
    public String description()
    {
        return "Generates a weekly overview of activities, extracted from the journal";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of(
            "folder", "Folder of notes to report on; defaults to '" + defaultFolder() + "'"
        );
    }

    @Override
    protected LocalDate resolveStartDate(QueryDefinition definition)
    {
        return parseWeeklyFrom(definition.document())
            .map(w -> journal().firstDayOf(w))
            .orElse(null);
    }

    @Override
    protected LocalDate resolveEndDate(QueryDefinition definition)
    {
        var startDate = resolveStartDate(definition);
        if (startDate == null)
        {
            return null;
        }
        return startDate.plusDays(7);
    }

    @Override
    protected Stream<Daily> resolveDailies(QueryDefinition definition)
    {
        var weekly = parseWeeklyFrom(definition.document());
        return weekly.map(w -> journal().dailiesForWeek(w)).orElse(Stream.empty());
    }
}
