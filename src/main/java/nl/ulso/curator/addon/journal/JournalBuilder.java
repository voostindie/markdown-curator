package nl.ulso.curator.addon.journal;

import nl.ulso.date.LocalDates;
import nl.ulso.curator.vault.*;

import java.time.LocalDate;
import java.util.*;

import static java.lang.Integer.parseInt;
import static nl.ulso.curator.addon.journal.Daily.parseDailiesFrom;
import static nl.ulso.curator.addon.journal.Weekly.DOCUMENT_NAME_PATTERN;
import static nl.ulso.curator.vault.Section.EMPTY_SECTION;

class JournalBuilder
    extends BreadthFirstVaultVisitor
{
    private final Set<Daily> dailies = new HashSet<>();
    private final Set<Weekly> weeklies = new HashSet<>();
    private final JournalSettings settings;
    private LocalDate currentDate = null;
    private boolean dailyFound = false;

    JournalBuilder(JournalSettings settings)
    {
        this.settings = settings;
    }

    static Optional<Weekly> parseWeeklyFrom(Document document)
    {
        var matcher = DOCUMENT_NAME_PATTERN.matcher(document.name());
        if (matcher.matches())
        {
            var year = parseInt(matcher.group(1));
            var week = parseInt(matcher.group(2));
            return Optional.of(new Weekly(year, week));
        }
        return Optional.empty();
    }

    static Optional<LocalDate> parseDateFrom(Document document)
    {
        return Optional.ofNullable(LocalDates.parseDateOrNull(document.name()));
    }

    @Override
    public void visit(Vault vault)
    {
        vault.folder(settings.journalFolderName()).ifPresent(journal -> journal.accept(this));
    }

    @Override
    public void visit(Document document)
    {
        parseWeeklyFrom(document).ifPresentOrElse(
            weeklies::add,
            () -> parseDateFrom(document).ifPresent(date -> {
                this.currentDate = date;
                dailyFound = false;
                super.visit(document);
                if (!dailyFound)
                {
                    dailies.add(parseDailiesFrom(EMPTY_SECTION, date)
                        .orElseThrow());
                }
            })
        );
    }

    @Override
    public void visit(Section section)
    {
        if (section.level() == 2 &&
            section.sortableTitle().contentEquals(settings.activitiesSectionName()))
        {
            dailyFound = true;
            parseDailiesFrom(section, currentDate).ifPresent(dailies::add);
        }
    }

    Set<Daily> dailies()
    {
        return dailies;
    }

    Set<Weekly> weeklies()
    {
        return weeklies;
    }
}
