package nl.ulso.curator.vault;

import java.nio.file.Path;

public interface DocumentPathResolver
{
    Path resolveAbsolutePath(Document document);
}
