package nl.ulso.curator.addon.journal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.EntityTransformer;
import nl.ulso.curator.vault.*;

import java.util.Optional;

import static nl.ulso.date.LocalDates.parseDateOrNull;

/// Creates dailies from documents in the journal folder.
@Singleton
final class DailyProducer
    extends EntityTransformer<Document, Daily>
{
    private final String journalFolderName;
    private final String activitiesSectionName;

    @Inject
    DailyProducer(JournalSettings settings)
    {
        this.journalFolderName = settings.journalFolderName();
        this.activitiesSectionName = settings.activitiesSectionName();
    }

    @Override
    protected Class<Document> sourceClass()
    {
        return Document.class;
    }

    @Override
    protected Class<Daily> targetClass()
    {
        return Daily.class;
    }

    @Override
    protected Optional<Daily> transform(Document document)
    {
        if (!document.isInPath(journalFolderName))
        {
            return Optional.empty();
        }
        var date = parseDateOrNull(document.name());
        if (date == null)
        {
            return Optional.empty();
        }
        var finder = new ActivitiesSectionFinder();
        document.accept(finder);
        var section = finder.section != null ? finder.section : Section.EMPTY_SECTION;
        return Optional.of(new Daily(date, section));
    }

    private class ActivitiesSectionFinder
        extends BreadthFirstVaultVisitor
    {
        private Section section;

        @Override
        public void visit(Section section)
        {
            if (this.section != null)
            {
                return;
            }
            if (section.level() == 2 &&
                section.sortableTitle().contentEquals(activitiesSectionName))
            {
                this.section = section;
            }
        }
    }
}
