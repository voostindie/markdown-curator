package nl.ulso.macu.vault;

import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

/**
 * Finds all internal links in the WikiLink format.
 * <p/>
 * Yes: internal links that look like normal Markdown links are considered to be external links.
 * <p/P
 * A full Obsidian WikiLink looks like this: [[document#anchor|label]], with the anchor and the
 * label optional.
 */
final class WikiLinkFinder
        extends BreadthFirstVaultVisitor
{
    private static final Pattern LINK_PATTERN = compile("\\[\\[(.*?)(?:#(.*?))?(?:\\|(.*))?]]");

    private final Set<InternalLink> internalLinks;

    WikiLinkFinder()
    {
        internalLinks = new HashSet<>();
    }

    Set<InternalLink> internalLinks()
    {
        return internalLinks;
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
        extractInternalLinks(textBlock, textBlock.content());
    }

    private void extractInternalLinks(Fragment fragment, String content)
    {
        allLinks(content).forEach(matchResult -> {
            var targetDocument = matchResult.group(1);
            var anchor = Optional.ofNullable(matchResult.group(2));
            var alias = Optional.ofNullable(matchResult.group(3));
            internalLinks.add(new InternalLink(
                    fragment,
                    targetDocument,
                    anchor,
                    alias));
        });
    }

    // This method is static for testing purposes
    static List<MatchResult> allLinks(String input)
    {
        return LINK_PATTERN.matcher(input).results().toList();
    }
}
