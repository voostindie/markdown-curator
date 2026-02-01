package nl.ulso.curator;

import nl.ulso.curator.vault.Document;

import java.nio.file.Path;

public interface DocumentPathResolver
{
    Path resolveAbsolutePath(Document document);
}
