package nl.ulso.obsidian.watcher.vault;

import java.util.Optional;

public record InternalLink(Location sourceLocation,
                           String targetDocument,
                           Optional<String> targetAnchor,
                           Optional<String> alias)
{
}
