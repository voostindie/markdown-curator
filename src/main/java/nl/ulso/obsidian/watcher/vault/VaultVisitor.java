package nl.ulso.obsidian.watcher.vault;

public interface VaultVisitor
{
    void visit(Vault vault);

    void visit(Folder folder);

    void visit(Document document);

    void visit(FrontMatter frontMatter);

    void visit(Section section);

    void visit(Text text);
}
