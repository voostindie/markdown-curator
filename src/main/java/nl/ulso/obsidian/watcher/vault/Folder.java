package nl.ulso.obsidian.watcher.vault;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.unmodifiableCollection;

public class Folder
{
    private final Folder parent;
    private final String name;
    private final Map<String, Folder> folders;
    private final Map<String, Document> documents;

    Folder(Folder parent, String name)
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

    void addDocument(Document document)
    {
        documents.put(document.name(), document);
    }

    void removeDocument(String name)
    {
        documents.remove(name);
    }
}
