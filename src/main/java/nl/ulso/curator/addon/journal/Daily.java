package nl.ulso.curator.addon.journal;

import nl.ulso.curator.addon.journal.Outline.LineValues;
import nl.ulso.curator.vault.*;

import java.time.LocalDate;
import java.util.*;

import static java.lang.Character.isWhitespace;
import static java.lang.System.lineSeparator;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.IntStream.range;
import static nl.ulso.curator.addon.journal.Outline.newOutline;
import static nl.ulso.curator.vault.InternalLinkFinder.findInternalLinks;
import static nl.ulso.date.LocalDates.parseDateOrNull;

/// Represents a single journal entry: an outline of lines for a certain date that reference other
/// documents in the vault.
///
/// When parsing a section into a journal entry, the lines in the section are temporarily mapped to
/// a tree structure ([Outline] to compute [LineValues]. These line values are used when generating
/// summaries for a document quickly.
///
/// The name of a daily in the vault MUST be formatted like "yyyy-MM-dd".
public class Daily
    implements Comparable<Daily>
{
    private final LocalDate date;
    private final Section section;
    private final List<LineValues> lineValues;
    private final Map<String, BitSet> documentReferences;

    private Daily(LocalDate date, Section section)
    {
        this.date = date;
        this.section = section;
        var sectionLines = sectionLines(section);
        this.documentReferences = extractDocumentReferences(section, sectionLines);
        this.lineValues = newOutline(sectionLines).toLineValues();
    }

    private List<String> sectionLines(Section section)
    {
        var collector = new LinesCollector();
        section.accept(collector);
        return collector.lines;
    }

    public static Optional<Daily> parseDailiesFrom(Section section)
    {
        return parseDailiesFrom(section, null);
    }

    public static Optional<Daily> parseDailiesFrom(Section section, LocalDate date)
    {
        if (date == null)
        {
            date = parseDateOrNull(section.document().name());
            if (date == null)
            {
                return Optional.empty();
            }
        }
        return Optional.of(new Daily(date, section));
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        var daily = (Daily) o;
        return date.equals(daily.date);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(date);
    }

    @Override
    public int compareTo(Daily other)
    {
        return date.compareTo(other.date);
    }

    public LocalDate date()
    {
        return date;
    }

    /// Generates a summary for the given document for this day.
    ///
    /// The summary contains all the lines that refer to the document, as well as the context of
    /// those lines: all lines up to the top-most line and all lines under the line, hierarchically.
    ///
    /// @param documentName Name of the document to create a summary for.
    /// @return The summary for the given document.
    public String summaryFor(String documentName)
    {
        var indexes = documentReferences.get(documentName);
        var usedIndexes = new HashSet<Integer>();
        var summary = new StringBuilder();
        var sectionLines = sectionLines(section);
        for (int selectedIndex = indexes.nextSetBit(0); selectedIndex >= 0;
             selectedIndex = indexes.nextSetBit(selectedIndex + 1))
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
                    summary.append(sectionLines.get(i)).append(lineSeparator());
                    usedIndexes.add(i);
                }
            }
        }
        return summary.toString();
    }

    /// Selects and transforms all lines for a document that have a specific marker in them; the
    /// results are grouped by marker, and optionally, the markers are removed from the lines.
    ///
    /// @param documentName  Name of the document to select all marked lines for.
    /// @param markerNames   Names of the markers to collect.
    /// @param removeMarkers Whether to remove the markers from the lines.
    /// @return A map of markers to lines.
    public Map<String, List<MarkedLine>> markedLinesFor(
        String documentName, Set<String> markerNames, boolean removeMarkers)
    {
        var result = new HashMap<String, List<MarkedLine>>();
        var indexes = documentReferences.get(documentName);
        var sectionLines = sectionLines(section);
        for (int selectedIndex = indexes.nextSetBit(0); selectedIndex >= 0;
             selectedIndex = indexes.nextSetBit(selectedIndex + 1))
        {
            var selectedLine = lineValues.get(selectedIndex);
            for (String marker : markerNames)
            {
                var markerIndexes = documentReferences.get(marker);
                if (markerIndexes == null)
                {
                    continue;
                }
                for (int markerIndex = markerIndexes.nextSetBit(0); markerIndex >= 0;
                     markerIndex = markerIndexes.nextSetBit(markerIndex + 1))
                {
                    var markerLine = lineValues.get(markerIndex);
                    if (!markerLine.isDirectChildOf(selectedLine))
                    {
                        continue;
                    }
                    var line = sectionLines.get(markerIndex).trim();
                    if (removeMarkers)
                    {
                        line = removeMarker(line, marker);
                    }
                    var list = result.computeIfAbsent(marker, _ -> new ArrayList<>());
                    list.add(new MarkedLine(date, line));
                }
            }
        }
        return result;
    }

    private String removeMarker(String line, String marker)
    {
        var start = line.indexOf("[[" + marker);
        var end = line.indexOf("]]", start + marker.length()) + 2;
        var length = line.length();
        while (end < length && isWhitespace(line.charAt(end)))
        {
            end++;
        }
        return line.substring(0, start) + line.substring(end);
    }

    public boolean refersTo(String documentName)
    {
        return documentReferences.containsKey(documentName);
    }

    Set<String> referencedDocuments()
    {
        return unmodifiableSet(documentReferences.keySet());
    }

    private Map<String, BitSet> extractDocumentReferences(
        Section section,
        List<String> sectionLines)
    {
        var references = new HashMap<String, BitSet>();
        var size = sectionLines.size();
        range(0, size).forEach(index ->
            findInternalLinks(section, sectionLines.get(index)).stream()
                .map(InternalLink::targetDocument)
                .collect(toSet())
                .forEach(documentName ->
                    references.computeIfAbsent(documentName,
                        (_ -> new BitSet(size))
                    ).set(index)));
        return unmodifiableMap(references);
    }

    private static class LinesCollector
        extends BreadthFirstVaultVisitor
    {
        final List<String> lines = new ArrayList<>();

        @Override
        public void visit(TextBlock textBlock)
        {
            textBlock.markdown().lines().forEach(lines::add);
        }
    }
}
