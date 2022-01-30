package nl.ulso.obsidian.watcher.vault;

public interface Visitor
{
    void visitVault(Vault vault);

    void visitFolder(Folder folder);

    void visitDocument(Document document);

    void visitFrontMatter(FrontMatter frontMatter);

    void visitSection(Section section);

    void visitText(Text text);
}
