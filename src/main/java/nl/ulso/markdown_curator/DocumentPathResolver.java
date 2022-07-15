package nl.ulso.markdown_curator;

import nl.ulso.markdown_curator.vault.Document;

import java.nio.file.Path;

public interface DocumentPathResolver
{
    Path resolveAbsolutePath(Document document);
}
