package nl.ulso.curator.addon.journal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.EntityTransformer;
import nl.ulso.curator.vault.Document;

import java.util.Optional;

import static nl.ulso.curator.addon.journal.Weekly.isWeekly;
import static nl.ulso.curator.addon.journal.Weekly.parseWeeklyFrom;

/// Creates weeklies from documents in the journal folder.
@Singleton
final class WeeklyProducer
    extends EntityTransformer<Document, Weekly>
{
    private final String journalFolderName;

    @Inject
    WeeklyProducer(JournalSettings journalSettings)
    {
        this.journalFolderName = journalSettings.journalFolderName();
    }

    @Override
    protected Class<Document> sourceClass()
    {
        return Document.class;
    }

    @Override
    protected Class<Weekly> targetClass()
    {
        return Weekly.class;
    }

    @Override
    protected Optional<Weekly> transform(Document document)
    {
        if (!document.isInPath(journalFolderName))
        {
            return Optional.empty();
        }
        if (!isWeekly(document))
        {
            return Optional.empty();
        }
        return parseWeeklyFrom(document);
    }
}
