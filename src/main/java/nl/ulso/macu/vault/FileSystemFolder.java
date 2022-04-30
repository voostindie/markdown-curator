package nl.ulso.macu.vault;

import java.util.*;

import static java.util.Collections.unmodifiableCollection;

/**
 * Represents a folder, containing documents and other folders.
 * <p/>
 * From the outside - the public API - this is a read-only structure. From the inside -
 * within this package - it's mutable so that the {@link Vault} can apply changes it detects
 * on disk.
 * <p/>
 */
public class FileSystemFolder
        implements Folder
{
    private final FileSystemFolder parent;
    private final String name;
    private final Map<String, Folder> folders;
    private final Map<String, Document> documents;

    /**
     * Construct a new Folder with the specified name; this is always a <strong>root</strong>
     * folder; it cannot be added to another folder as a subfolder.
     * <p/>
     * To add subfolders to a folder, use {@link #addFolder(String)}.
     */
    FileSystemFolder(String name)
    {
        this(null, name);
    }

    private FileSystemFolder(FileSystemFolder parent, String name)
    {
        this.parent = parent;
        this.name = name;
        this.folders = new HashMap<>();
        this.documents = new HashMap<>();
    }

    @Override
    public final boolean equals(Object o)
    {
        if (o instanceof FileSystemFolder folder)
        {
            return Objects.equals(name, folder.name)
                    && Objects.equals(parent, folder.parent);
        }
        return false;
    }

    @Override
    public final int hashCode()
    {
        return Objects.hash(name, parent);
    }

    @Override
    public FileSystemFolder parent()
    {
        if (parent == null)
        {
            throw new IllegalStateException(name + " is the root folder. You can't go further up!");
        }
        return parent;
    }

    @Override
    public String name()
    {
        return name;
    }

    @Override
    public Collection<Folder> folders()
    {
        return unmodifiableCollection(folders.values());
    }

    @Override
    public Optional<Folder> folder(String name)
    {
        return Optional.ofNullable(folders.get(name));
    }

    @Override
    public Collection<Document> documents()
    {
        return unmodifiableCollection(documents.values());
    }

    @Override
    public Optional<Document> document(String name)
    {
        return Optional.ofNullable(documents.get(name));
    }

    @Override
    public void accept(VaultVisitor visitor)
    {
        visitor.visit(this);
    }

    /**
     * Add a subfolder with the specified name. If a folder with the same name already exists, it is
     * replaced. That means that all documents and folders within it are lost!
     */
    FileSystemFolder addFolder(String name)
    {
        var folder = new FileSystemFolder(this, name);
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
        document.setFolder(this);
        documents.put(document.name(), document);
    }

    void removeDocument(String name)
    {
        documents.remove(name);
    }
}
