package nl.ulso.markdown_curator.vault;

public interface VaultVisitor
{
    void visit(Vault vault);

    void visit(Folder folder);

    void visit(Document document);

    void visit(FrontMatter frontMatter);

    void visit(Section section);

    void visit(CodeBlock codeBlock);

    void visit(QueryBlock queryBlock);

    void visit(TextBlock textBlock);
}
