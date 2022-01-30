package nl.ulso.obsidian.watcher.vault;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    Document document(String name)
    {
        return documents.get(name);
    }

    Folder folder(String name)
    {
        return folders.get(name);
    }

    public void accept(Visitor visitor)
    {
        visitor.visitFolder(this);
    }

    Folder addFolder(Folder folder)
    {
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
