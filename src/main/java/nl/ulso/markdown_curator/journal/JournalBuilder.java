package nl.ulso.markdown_curator.journal;

import nl.ulso.markdown_curator.vault.*;

import java.time.LocalDate;
import java.util.*;

import static java.lang.Integer.parseInt;
import static nl.ulso.markdown_curator.journal.Daily.parseDailiesFrom;
import static nl.ulso.markdown_curator.journal.Weekly.DOCUMENT_NAME_PATTERN;

class JournalBuilder
        extends BreadthFirstVaultVisitor
{
    private final Set<Daily> dailies = new HashSet<>();
    private final Set<Weekly> weeklies = new HashSet<>();
    private final JournalSettings settings;
    private LocalDate currentDate = null;

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
        var weekly = parseWeeklyFrom(document);
        weekly.ifPresentOrElse(weeklies::add, () ->
        {
            parseDateFrom(document).ifPresent(date ->
            {
                this.currentDate = date;
                super.visit(document);
            });
        });
    }

    @Override
    public void visit(Section section)
    {
        if (section.level() == 2 &&
            section.sortableTitle().contentEquals(settings.activitiesSectionName()))
        {
            parseDailiesFrom(section, currentDate).ifPresent(dailies::add);
        }
    }

    Set<Daily> dailies()
    {
        return dailies;
    }

    Set<Weekly> weeklies() {return weeklies;}
}
