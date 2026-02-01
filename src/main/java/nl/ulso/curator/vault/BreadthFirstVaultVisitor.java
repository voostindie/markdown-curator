package nl.ulso.curator.vault;

/// Abstract implementation of the VaultVisitor that implements a breadth-first traversal of a vault.
/// This visitor processes all elements of all documents in all folders in a vault.
public abstract class BreadthFirstVaultVisitor
        implements VaultVisitor
{
    @Override
    public void visit(Vault vault)
    {
        vault.documents().forEach(d -> d.accept(this));
        vault.folders().forEach(f -> f.accept(this));
    }

    @Override
    public void visit(Folder folder)
    {
        folder.documents().forEach(d -> d.accept(this));
        folder.folders().forEach(f -> f.accept(this));
    }

    @Override
    public void visit(Document document)
    {
        document.fragments().forEach(f -> f.accept(this));
    }

    @Override
    public void visit(FrontMatter frontMatter)
    {

    }

    @Override
    public void visit(Section section)
    {
        section.fragments().forEach(f -> f.accept(this));
    }

    @Override
    public void visit(CodeBlock codeBlock)
    {

    }

    @Override
    public void visit(QueryBlock queryBlock)
    {

    }

    @Override
    public void visit(TextBlock textBlock)
    {

    }
}
