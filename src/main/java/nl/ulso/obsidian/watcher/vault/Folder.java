package nl.ulso.obsidian.watcher.vault;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.unmodifiableCollection;

/**
 * Represents a folder, containing documents and other folders.
 * <p/>
 * From the outside - the public API - this is a read-only structure. From the inside -
 * within this package - it's mutable so that the {@link Vault} can apply changes it detects
 * on disk.
 * <p/>
 * This data structure can be used in a multi-threaded environment.
 */
public class Folder
{
    private final Folder parent;
    private final String name;
    private final Map<String, Folder> folders;
    private final Map<String, Document> documents;

    /**
     * Construct a new Folder with the specified name; this is always a <strong>root</strong>
     * folder; it cannot be added to another folder as a subfolder.
     * <p/>
     * To add subfolders to a folder, use {@link #addFolder(String)}.
     */
    Folder(String name)
    {
        this(null, name);
    }

    private Folder(Folder parent, String name)
    {
        this.parent = parent;
        this.name = name;
        this.folders = new ConcurrentHashMap<>();
        this.documents = new ConcurrentHashMap<>();
    }

    @Override
    public final boolean equals(Object o)
    {
        if (o instanceof Folder folder)
        {
            return Objects.equals(name, folder.name)
                    && Objects.equals(parent, folder.parent)
                    && Objects.equals(folders, folder.folders)
                    && Objects.equals(documents, folder.documents);
        }
        return false;
    }

    @Override
    public final int hashCode()
    {
        return Objects.hash(name, parent, folders, documents);
    }

    public Folder parent()
    {
        if (parent == null)
        {
            throw new IllegalStateException(name + " is the root folder. You can't go further up!");
        }
        return parent;
    }

    public String name()
    {
        return name;
    }

    public Collection<Folder> folders()
    {
        return unmodifiableCollection(folders.values());
    }

    public Optional<Folder> folder(String name)
    {
        return Optional.ofNullable(folders.get(name));
    }

    public Collection<Document> documents()
    {
        return unmodifiableCollection(documents.values());
    }

    public Optional<Document> document(String name)
    {
        return Optional.ofNullable(documents.get(name));
    }

    public void accept(Visitor visitor)
    {
        visitor.visitFolder(this);
    }

    /**
     * Add a subfolder with the specified name. If a folder with the same name already exists, it is
     * replaced. That means that all documents and folders within it are lost!
     */
    Folder addFolder(String name)
    {
        var folder = new Folder(this, name);
        folders.put(folder.name(), folder);
        return folder;
    }

    void removeFolder(String name)
    {
        folders.remove(name);
    }

    /**
     * Add a document to the current folder. If a document with the same name already exists, it is
     * replaced.
     */
    void addDocument(Document document)
    {
        documents.put(document.name(), document);
    }

    void removeDocument(String name)
    {
        documents.remove(name);
    }
}
