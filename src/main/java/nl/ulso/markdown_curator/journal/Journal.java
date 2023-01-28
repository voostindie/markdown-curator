package nl.ulso.markdown_curator.journal;

import nl.ulso.markdown_curator.DataModelTemplate;
import nl.ulso.markdown_curator.vault.*;
import nl.ulso.markdown_curator.vault.event.*;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.reverseOrder;
import static org.slf4j.LoggerFactory.getLogger;

@Singleton
public class Journal
        extends DataModelTemplate
{
    private static final Logger LOGGER = getLogger(Journal.class);

    private final Vault vault;
    private final JournalSettings settings;
    private final Map<LocalDate, JournalEntry> entries;

    @Inject
    Journal(Vault vault, JournalSettings settings)
    {
        this.vault = vault;
        this.settings = settings;
        this.entries = new HashMap<>();
    }

    @Override
    protected void fullRefresh()
    {
        var builder = new JournalBuilder(settings);
        vault.accept(builder);
        entries.clear();
        builder.entries().forEach(entry -> entries.put(entry.date(), entry));
        LOGGER.debug("Built a journal for {} days", entries.size());
    }

    @Override
    public void process(DocumentAdded event)
    {
        processDocumentUpdate(event.document());
    }

    @Override
    public void process(DocumentChanged event)
    {
        processDocumentUpdate(event.document());
    }

    private void processDocumentUpdate(Document document)
    {
        if (isJournalEntry(document))
        {
            var builder = new JournalBuilder(settings);
            document.accept(builder);
            builder.entries().forEach(entry -> entries.put(entry.date(), entry));
            LOGGER.debug("Updated the journal for {}", document.name());
        }
    }

    @Override
    public void process(DocumentRemoved event)
    {
        var document = event.document();
        if (isJournalEntry(document))
        {
            var date = LocalDates.parseDateOrNull(document.name());
            if (date != null)
            {
                entries.remove(date);
                LOGGER.debug("Removed date {} from the journal", date);
            }
        }
    }

    private boolean isJournalEntry(Document document)
    {
        return document.folder().name().contentEquals(settings.journalFolderName());
    }

    public SortedMap<LocalDate, String> timelineFor(String documentName)
    {
        var timeline = new TreeMap<LocalDate, String>(reverseOrder());
        journalEntriesFor(documentName)
                .forEach(entry -> timeline.put(entry.date(), entry.summaryFor(documentName)));
        return timeline;
    }

    public Optional<LocalDate> mostRecentMentionOf(String documentName)
    {
        return journalEntriesFor(documentName)
                .map(JournalEntry::date)
                .max(naturalOrder());
    }

    private Stream<JournalEntry> journalEntriesFor(String documentName)
    {
        return entries.values().stream()
                .filter(entry -> entry.refersTo(documentName));
    }

    Vault vault()
    {
        return vault;
    }
}
