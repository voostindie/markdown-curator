package nl.ulso.macu.vault;

import java.util.Optional;

public record InternalLink(Fragment sourceLocation,
                           String targetDocument,
                           Optional<String> targetAnchor,
                           Optional<String> alias)
{
}
