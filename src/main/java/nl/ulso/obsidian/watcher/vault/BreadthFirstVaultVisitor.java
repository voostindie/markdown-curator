package nl.ulso.obsidian.watcher.vault;

import java.util.*;

/**
 * Abstract implementation of the VaultVisitor that implements a breadth first traversal of a vault.
 * This visitor processes all elements of all documents in all folders in a vault.
 * <p/>
 * The visitor keeps track of where it is in the vault tree as well as within a document:
 * <ul>
 *     <li>{@link #currentPath()} returns the path to the current document.</li>
 *     <li>{@link #currentDocument()} returns the current document itself.</li>
 *     <li>{@link #currentLocation()} returns the location within the current document.</li>
 * </ul>
 */
public abstract class BreadthFirstVaultVisitor
        implements VaultVisitor
{
    private LinkedList<Folder> currentPath = null;
    private Document currentDocument = null;
    private LinkedList<Section> currentLocation = null;

    protected List<Folder> currentPath()
    {
        return Collections.unmodifiableList(Objects.requireNonNull(currentPath));
    }

    protected Document currentDocument()
    {
        return Objects.requireNonNull(currentDocument);
    }

    protected List<Section> currentLocation()
    {
        return Collections.unmodifiableList(Objects.requireNonNull(currentLocation));
    }

    @Override
    public void visit(Vault vault)
    {
        currentPath = new LinkedList<>();
        vault.documents().forEach(d -> d.accept(this));
        vault.folders().forEach(f -> f.accept(this));
        currentPath = null;
    }

    @Override
    public void visit(Folder folder)
    {
        if (currentPath != null)
        {
            currentPath.addLast(folder);
        }
        folder.documents().forEach(d -> d.accept(this));
        folder.folders().forEach(f -> f.accept(this));
        if (currentPath != null)
        {
            currentPath.removeLast();
        }
    }

    @Override
    public void visit(Document document)
    {
        currentDocument = document;
        currentLocation = new LinkedList<>();
        document.fragments().forEach(f -> f.accept(this));
        currentDocument = null;
        currentLocation = null;
    }

    @Override
    public void visit(FrontMatter frontMatter)
    {

    }

    @Override
    public void visit(Section section)
    {
        if (currentLocation != null)
        {
            currentLocation.addLast(section);
        }
        section.fragments().forEach(f -> f.accept(this));
        if (currentLocation != null)
        {
            currentLocation.removeLast();
        }
    }

    @Override
    public void visit(Text text)
    {

    }
}
