package nl.ulso.markdown_curator.journal;

import nl.ulso.markdown_curator.vault.*;

import java.util.HashSet;
import java.util.Set;

import static nl.ulso.markdown_curator.journal.JournalEntry.parseJournalEntryFrom;

class JournalBuilder
        extends BreadthFirstVaultVisitor
{
    private final Set<JournalEntry> journal = new HashSet<>();
    private final JournalSettings settings;

    JournalBuilder(JournalSettings settings)
    {
        this.settings = settings;
    }

    @Override
    public void visit(Vault vault)
    {
        vault.folder(settings.journalFolderName()).ifPresent(journal -> journal.accept(this));
    }

    @Override
    public void visit(Section section)
    {
        if (section.level() == 2 &&
            section.sortableTitle().contentEquals(settings.activitiesSectionName()))
        {
            parseJournalEntryFrom(section).ifPresent(journal::add);
        }
    }

    Set<JournalEntry> journal()
    {
        return journal;
    }
}
