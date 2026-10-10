package nl.ulso.curator.query;

import nl.ulso.curator.vault.DocumentPathResolver;
import nl.ulso.curator.vault.Folder;

record FolderResult(
    Folder folder,
    DocumentPathResolver documentPathResolver
)
    implements QueryResult
{
}
