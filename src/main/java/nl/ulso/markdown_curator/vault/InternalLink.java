package nl.ulso.markdown_curator.vault;

import java.util.Optional;

public record InternalLink(Fragment sourceLocation,
                           String targetDocument,
                           Optional<String> targetAnchor,
                           Optional<String> alias)
{
    /**
     * @return The internal link as a Markdown string.
     */
    public String toMarkdown()
    {
        var builder = new StringBuilder();
        builder.append("[[").append(targetDocument());
        targetAnchor.ifPresent((anchor) -> builder.append("#").append(anchor));
        alias.ifPresent((alias) -> builder.append("|").append(alias));
        builder.append("]]");
        return builder.toString();
    }
}
