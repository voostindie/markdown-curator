package nl.ulso.curator.addon.journal;

import java.time.LocalDate;
import java.util.Optional;

/// Keeps track of weekly journal entries.
interface WeeklyRepository
{
    Optional<Weekly> weeklyBefore(Weekly weekly);

    Optional<Weekly> weeklyAfter(Weekly weekly);

    Optional<Weekly> weeklyFor(LocalDate date);

    LocalDate firstDayOf(Weekly weekly);

    Weekly computeWeeklyFor(LocalDate date);

    int dayOfWeekNumberFor(LocalDate date);
}
