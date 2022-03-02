package nl.ulso.obsidian.watcher.vault;

import java.util.List;
import java.util.Optional;

public record InternalLink(Document sourceDocument,
                           List<Section> sourceLocation,
                           String targetDocument,
                           Optional<String> targetAnchor,
                           Optional<String> alias)
{
}
