package nl.ulso.macu.curator.rabobank;

import nl.ulso.macu.vault.Section;

import java.time.LocalDate;

record JournalEntry(LocalDate date, String folder, Section section, String subject)
{
}
