package nl.ulso.curator.addon.journal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.SetBasedEntityRepository;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;

@Singleton
final class DefaultWeeklyRepository
    extends SetBasedEntityRepository<Weekly>
    implements WeeklyRepository
{
    private final WeekFields weekFields;

    @Inject
    DefaultWeeklyRepository(JournalSettings settings)
    {
        this.weekFields = settings.weekFields();
    }

    @Override
    protected Class<Weekly> entityClass()
    {
        return Weekly.class;
    }

    @Override
    protected Class<?> repositoryClass()
    {
        return WeeklyRepository.class;
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
}
