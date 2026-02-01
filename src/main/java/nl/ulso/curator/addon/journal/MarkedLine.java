package nl.ulso.curator.addon.journal;

import java.time.LocalDate;

public record MarkedLine(LocalDate date, String line)
{
}
