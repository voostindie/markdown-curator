package nl.ulso.markdown_curator.vault;

import java.util.*;

import static nl.ulso.markdown_curator.vault.Document.newDocument;

public class FolderStub
        implements Folder
{
    private final FolderStub parent;
    private final String name;
    private final Map<String, FolderStub> folders = new HashMap<>();
    private final Map<String, Document> documents = new HashMap<>();

    public FolderStub(FolderStub parent, String name)
    {
        this.parent = parent;
        this.name = name;
    }

    public FolderStub addFolder(String name)
    {
        var folder = new FolderStub(this, name);
        folders.put(name, folder);
        return folder;
    }

    public Document addDocumentInPath(String path, String content)
    {
        int index = 0;
        int length = path.length();
        FolderStub folder = this;
        while (index < length)
        {
            int folderEnd = path.indexOf('/', index);
            if (folderEnd == -1)
            {
                break;
            }
            var folderName = path.substring(index, folderEnd);
            final FolderStub parent = folder;
            folder = parent.folders.computeIfAbsent(folderName, s -> new FolderStub(parent, s));
            index = folderEnd + 1;
        }
        var documentName = path.substring(index);
        return folder.addDocument(documentName, content);
    }

    public Document addDocument(String name, String content)
    {
        var document = newDocument(name, 0, content.lines().toList());
        document.setFolder(this);
        documents.put(name, document);
        return document;
    }

    public Document resolveDocumentInPath(String path)
    {
        int index = 0;
        int length = path.length();
        FolderStub folder = this;
        while (index < length)
        {
            int folderEnd = path.indexOf('/', index);
            if (folderEnd == -1)
            {
                break;
            }
            var folderName = path.substring(index, folderEnd);
            folder = folder.folders.get(folderName);
            if (folder == null)
            {
                throw new IllegalStateException("Folder doesn't exist: " + folderName);
            }
            index = folderEnd + 1;
        }
        var documentName = path.substring(index);
        return folder.document(documentName).orElseThrow();
    }


    @Override
    public Folder parent()
    {
        if (parent == null)
        {
            throw new IllegalStateException("Root folder has no parent");
        }
        return parent;
    }

    @Override
    public String name()
    {
        return name;
    }

    @Override
    public void accept(VaultVisitor visitor)
    {
        visitor.visit(this);
    }

    @Override
    public Collection<Folder> folders()
    {
        return folders.values().stream().map(fs -> (Folder) (fs)).toList();
    }

    @Override
    public Optional<Folder> folder(String name)
    {
        return Optional.ofNullable(folders.get(name));
    }

    @Override
    public Collection<Document> documents()
    {
        return documents.values();
    }

    @Override
    public Optional<Document> document(String name)
    {
        return Optional.ofNullable(documents.get(name));
    }

    @Override
    public Optional<Document> findDocument(String name)
    {
        var finder = new DocumentFinder(name);
        accept(finder);
        return finder.document();
    }

    @Override
    public String toString()
    {
        return name;
    }
}
