package nl.ulso.macu.vault;

import java.util.Collection;
import java.util.Optional;

/**
 * Represents a folder, containing documents and other folders.
 */
public interface Folder
{
    Folder parent();

    String name();

    void accept(VaultVisitor visitor);

    Collection<Folder> folders();

    Optional<Folder> folder(String name);

    Collection<Document> documents();

    Optional<Document> document(String name);
}
