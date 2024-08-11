package nl.ulso.markdown_curator.journal;

import java.time.LocalDate;

public record MarkedLine(LocalDate date, String line)
{
}
