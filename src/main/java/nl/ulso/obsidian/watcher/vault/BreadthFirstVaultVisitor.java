package nl.ulso.obsidian.watcher.vault;

import java.util.LinkedList;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * Abstract implementation of the VaultVisitor that implements a breadth first traversal of a vault.
 * This visitor processes all elements of all documents in all folders in a vault.
 * <p/>
 * The visitor keeps track of where it is in the vault tree as well as within a document:
 * <ul>
 *     <li>{@link #currentLocation()} returns the current location as a single object.</li>
 *     <li>{@link #currentVaultPath()} returns the path to the current document.</li>
 *     <li>{@link #currentDocument()} returns the current document itself.</li>
 *     <li>{@link #currentDocumentPath()} returns the sourceLocation within the current document
 *     .</li>
 * </ul>
 */
public abstract class BreadthFirstVaultVisitor
        implements VaultVisitor
{
    private LinkedList<Folder> vaultPath = null;
    private Document document = null;
    private LinkedList<Section> documentPath = null;

    protected List<Folder> currentVaultPath()
    {
        return unmodifiableList(requireNonNull(vaultPath,
                "To keep track of the vaultPath, start this visitor on the vault level."));
    }

    protected Document currentDocument()
    {
        return requireNonNull(document,
                "To keep track of the document, start this visitor on the document level.");
    }

    protected List<Section> currentDocumentPath()
    {
        return unmodifiableList(requireNonNull(documentPath,
                "To keep track of the sourceLocation, start this visitor on the document level."));
    }

    protected Location currentLocation()
    {
        return new Location(vaultPath, document, documentPath);
    }

    @Override
    public void visit(Vault vault)
    {
        vaultPath = new LinkedList<>();
        vault.documents().forEach(d -> d.accept(this));
        vault.folders().forEach(f -> f.accept(this));
        vaultPath = null;
    }

    @Override
    public void visit(Folder folder)
    {
        if (vaultPath != null)
        {
            vaultPath.addLast(folder);
        }
        folder.documents().forEach(d -> d.accept(this));
        folder.folders().forEach(f -> f.accept(this));
        if (vaultPath != null)
        {
            vaultPath.removeLast();
        }
    }

    @Override
    public void visit(Document document)
    {
        this.document = document;
        documentPath = new LinkedList<>();
        document.fragments().forEach(f -> f.accept(this));
        this.document = null;
        documentPath = null;
    }

    @Override
    public void visit(FrontMatter frontMatter)
    {

    }

    @Override
    public void visit(Section section)
    {
        if (documentPath != null)
        {
            documentPath.addLast(section);
        }
        section.fragments().forEach(f -> f.accept(this));
        if (documentPath != null)
        {
            documentPath.removeLast();
        }
    }

    @Override
    public void visit(CodeBlock codeBlock)
    {

    }

    @Override
    public void visit(Query query)
    {

    }

    @Override
    public void visit(Text text)
    {

    }
}
