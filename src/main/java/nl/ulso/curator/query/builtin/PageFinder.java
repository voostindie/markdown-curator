package nl.ulso.curator.query.builtin;

import nl.ulso.curator.vault.*;

import java.util.ArrayList;
import java.util.List;

/// Finds all pages in a folder, optionally recursively.
///
/// @see ListQuery
/// @see TableQuery
class PageFinder
        extends BreadthFirstVaultVisitor
{
    private final List<Document> pages;
    private final String selectedFolder;
    private final boolean recurse;

    private boolean inFolder;

    PageFinder(String selectedFolder, boolean recurse)
    {
        this.pages = new ArrayList<>();
        this.selectedFolder = selectedFolder;
        this.recurse = recurse;
        this.inFolder = false;
    }

    List<Document> pages()
    {
        return pages;
    }

    @Override
    public void visit(Folder folder)
    {
        if (!inFolder && folder.name().contentEquals(selectedFolder))
        {
            inFolder = true;
            super.visit(folder);
            inFolder = false;
        }
        if (inFolder && recurse)
        {
            super.visit(folder);
        }
    }

    @Override
    public void visit(Document document)
    {
        if (inFolder)
        {
            pages.add(document);
        }
    }
}
