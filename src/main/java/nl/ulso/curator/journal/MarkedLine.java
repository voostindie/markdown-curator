package nl.ulso.curator.journal;

import java.time.LocalDate;

public record MarkedLine(LocalDate date, String line)
{
}
