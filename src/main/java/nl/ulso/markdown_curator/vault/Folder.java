package nl.ulso.markdown_curator.vault;

import java.util.Collection;
import java.util.Optional;

/**
 * Represents a folder, containing documents and other folders.
 */
public interface Folder
    extends Visitable
{
    Folder parent();

    String name();

    Collection<Folder> folders();

    Optional<Folder> folder(String name);

    Collection<Document> documents();

    Optional<Document> document(String name);
}
