package nl.ulso.curator.addon.journal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.SetBasedEntityRepository;
import nl.ulso.curator.vault.Document;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;

import static nl.ulso.curator.addon.journal.Weekly.isWeekly;
import static nl.ulso.curator.addon.journal.Weekly.parseWeeklyFrom;

@Singleton
final class DefaultWeeklyRepository
    extends SetBasedEntityRepository<Document, Weekly>
    implements WeeklyRepository
{
    private final String journalFolderName;
    private final WeekFields weekFields;

    @Inject
    DefaultWeeklyRepository(JournalSettings settings)
    {
        this.journalFolderName = settings.journalFolderName();
        this.weekFields = settings.weekFields();
    }

    @Override
    protected Class<Document> sourceEntityClass()
    {
        return Document.class;
    }

    @Override
    protected Class<Weekly> targetEntityClass()
    {
        return Weekly.class;
    }

    @Override
    protected boolean isEntity(Document document)
    {
        return document.isInPath(journalFolderName) && isWeekly(document);
    }

    @Override
    protected Weekly createEntityFrom(Document document)
    {
        return parseWeeklyFrom(document).orElseThrow();
    }

    @Override
    protected Set<Weekly> createSet()
    {
        return new TreeSet<>();
    }

    @Override
    public Optional<Weekly> weeklyBefore(Weekly weekly)
    {
        return Optional.ofNullable(navigableSet().lower(weekly));
    }

    @Override
    public Optional<Weekly> weeklyAfter(Weekly weekly)
    {
        return Optional.ofNullable(navigableSet().higher(weekly));
    }

    @Override
    public Optional<Weekly> weeklyFor(LocalDate date)
    {
        var weekly = computeWeeklyFor(date);
        if (set().contains(weekly))
        {
            return Optional.of(weekly);
        }
        return Optional.empty();
    }

    @Override
    public LocalDate firstDayOf(Weekly weekly)
    {
        return LocalDate.now().with(weekFields.weekBasedYear(), weekly.year())
            .with(weekFields.weekOfWeekBasedYear(), weekly.week())
            .with(weekFields.dayOfWeek(), weekFields.getFirstDayOfWeek().getValue());
    }

    @Override
    public Weekly computeWeeklyFor(LocalDate date)
    {
        var year = date.get(weekFields.weekBasedYear());
        var week = date.get(weekFields.weekOfWeekBasedYear());
        return new Weekly(year, week);
    }

    @Override
    public int dayOfWeekNumberFor(LocalDate date)
    {
        return date.get(weekFields.dayOfWeek());
    }

    @Override
    public String name()
    {
        return WeeklyRepository.class.getSimpleName();
    }
}
