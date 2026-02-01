package nl.ulso.curator.vault;

import java.util.*;

import static java.util.stream.Collectors.toSet;

/// Finds all internal links in the WikiLink format.
///
/// Yes: internal links that look like normal Markdown links are considered to be external links.
/// <p/P A full Obsidian WikiLink looks like this: `[[document#anchor|label]]`, with the anchor and
/// the label optional.
public class InternalLinkFinder
    extends BreadthFirstVaultVisitor
{
    private static final String LINK_START = "[[";
    private static final int LINK_START_LENGTH = LINK_START.length();
    private static final String LINK_END = "]]";
    private static final int LINK_END_LENGTH = LINK_END.length();

    private static final char ANCHOR_MARKER = '#';
    private static final char ALIAS_MARKER = '|';

    private final List<InternalLink> internalLinks;

    public InternalLinkFinder()
    {
        internalLinks = new ArrayList<>();
    }

    public List<InternalLink> internalLinks()
    {
        return internalLinks;
    }

    /// Extracts all references to internal documents - target names only - from a chunk of
    /// Markdown.
    ///
    /// @param content Markdown text to extract internal links from.
    /// @return Set of references to internal documents.
    public static Set<String> parseInternalLinkTargetNames(String content)
    {
        return parseInternalLinks(null, content).stream()
            .map(InternalLink::targetDocument)
            .collect(toSet());
    }

    public static List<InternalLink> findInternalLinks(Fragment fragment, String content)
    {
        var finder = new InternalLinkFinder();
        finder.extractInternalLinks(fragment, content);
        return finder.internalLinks();
    }

    private static List<InternalLink> parseInternalLinks(Fragment fragment, String content)
    {
        var result = new ArrayList<InternalLink>();
        var index = 0;
        var length = content.length();
        while (index < length)
        {
            var start = content.indexOf(LINK_START, index);
            if (start == -1)
            {
                break;
            }
            var end = content.indexOf(LINK_END, start + LINK_START_LENGTH);
            if (end == -1)
            {
                break;
            }
            var link = content.substring(start + LINK_START_LENGTH, end);
            Optional<String> alias = Optional.empty();
            var marker = link.indexOf(ALIAS_MARKER);
            if (marker != -1)
            {
                alias = Optional.of(link.substring(marker + 1));
                link = link.substring(0, marker);
            }
            Optional<String> anchor = Optional.empty();
            marker = link.indexOf(ANCHOR_MARKER);
            if (marker != -1)
            {
                anchor = Optional.of(link.substring(marker + 1));
                link = link.substring(0, marker);
            }
            result.add(new InternalLink(
                fragment,
                link,
                anchor,
                alias
            ));
            index = end + LINK_END_LENGTH;
        }
        return result;
    }

    @Override
    public void visit(Section section)
    {
        extractInternalLinks(section, section.title());
        super.visit(section);
    }

    @Override
    public void visit(TextBlock textBlock)
    {
        extractInternalLinks(textBlock, textBlock.markdown());
    }

    protected void extractInternalLinks(Fragment fragment, String content)
    {
        internalLinks.addAll(parseInternalLinks(fragment, content));
    }
}
