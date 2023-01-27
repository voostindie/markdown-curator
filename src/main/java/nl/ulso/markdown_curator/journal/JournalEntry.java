package nl.ulso.markdown_curator.journal;

import nl.ulso.markdown_curator.journal.Outline.LineValues;
import nl.ulso.markdown_curator.vault.*;

import java.time.LocalDate;
import java.util.*;

import static java.lang.System.lineSeparator;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toSet;
import static nl.ulso.markdown_curator.journal.Outline.newOutline;
import static nl.ulso.markdown_curator.vault.InternalLinkFinder.findInternalLinks;

/**
 * Represents a single journal entry: on outline of lines for a certain date that reference other
 * documents in the vault.
 * <p/>
 * When parsing a section into a journal entry, the lines in the section are temporarily mapped
 * to a tree structure ({@link Outline}, in order to compute {@link LineValues}. These line values
 * are used when generating summaries for a document quickly.
 */
public class JournalEntry
        implements Comparable<JournalEntry>
{
    private final LocalDate date;
    private final Section section;
    private final List<LineValues> lineValues;
    private final Map<String, Set<Integer>> documentReferences;

    private JournalEntry(LocalDate date, Section section)
    {
        this.date = date;
        this.section = section;
        this.documentReferences = extractDocumentReferences(section);
        this.lineValues = newOutline(section.lines()).toLineValues();
    }

    public static Optional<JournalEntry> parseJournalEntryFrom(Section section)
    {
        var date = LocalDates.parseDateOrNull(section.document().name());
        if (date == null)
        {
            return Optional.empty();
        }
        return Optional.of(new JournalEntry(date, section));
    }

    @Override
    public int compareTo(JournalEntry other)
    {
        return date.compareTo(other.date);
    }

    public LocalDate date()
    {
        return date;
    }

    /**
     * Generates a summary for the given document for this day.
     * <p/>
     * The summary contains all the lines that refer to the document, as well as the context of
     * those lines: all lines up to the top-most line and all lines under the line, hierarchically.
     *
     * @param documentName Name of the document to create a summary for.
     * @return The summary for the given document.
     */
    public String summaryFor(String documentName)
    {
        var indexes = documentReferences.get(documentName).stream().sorted().toList();
        var usedIndexes = new HashSet<Integer>();
        var summary = new StringBuilder();
        for (Integer selectedIndex : indexes)
        {
            if (usedIndexes.contains(selectedIndex))
            {
                continue;
            }
            var selectedLine = lineValues.get(selectedIndex);
            var size = lineValues.size();
            for (var i = 0; i < size; i++)
            {
                if (!usedIndexes.contains(i) && selectedLine.includesInSummary(lineValues.get(i)))
                {
                    summary.append(section.lines().get(i)).append(lineSeparator());
                    usedIndexes.add(i);
                }
            }
        }
        return summary.toString();
    }

    public boolean refersTo(String documentName)
    {
        return documentReferences.containsKey(documentName);
    }

    private Map<String, Set<Integer>> extractDocumentReferences(Section section)
    {
        var references = new HashMap<String, Set<Integer>>();
        var lines = section.lines();
        var size = lines.size();
        for (int i = 0; i < size; i++)
        {
            var referencedDocuments = findInternalLinks(section, lines.get(i)).stream()
                    .map(InternalLink::targetDocument)
                    .collect(toSet());
            for (String documentName : referencedDocuments)
            {
                references.computeIfAbsent(documentName, (name -> new HashSet<>())).add(i);
            }
        }
        return unmodifiableMap(references);
    }
}
