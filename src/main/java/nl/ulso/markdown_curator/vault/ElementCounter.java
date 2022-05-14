package nl.ulso.markdown_curator.vault;

import static nl.ulso.markdown_curator.vault.ElementCounter.Scope.ALL;
import static nl.ulso.markdown_curator.vault.ElementCounter.Scope.FOLDERS_AND_DOCUMENTS;

/**
 * Simple visitor that counts all occurrences of the different types.
 * <p/>
 * It supports two search scopes:
 * <ol>
 * <li>{@literal ALL}: the default; it counts all elements, on all levels.</li>
 * <li>{@literal FOLDERS_AND_DOCUMENTS}: counts only folders and documents.</li>
 * </ol>
 */
public final class ElementCounter
        extends BreadthFirstVaultVisitor
{
    public enum Scope
    {
        FOLDERS_AND_DOCUMENTS,
        ALL
    }

    public record Statistics(
            int vaults,
            int folders,
            int documents,
            int frontMatters,
            int sections,
            int texts,
            int queries,
            int codeBlocks
    ) {}

    private final Scope scope;
    private int vaults;
    private int folders;
    private int documents;
    private int frontMatters;
    private int sections;
    private int texts;
    private int queries;
    private int codeBlocks;

    ElementCounter(Scope scope)
    {
        this.scope = scope;
    }

    private static Statistics count(Vault vault, Scope scope)
    {
        var counter = new ElementCounter(scope);
        vault.accept(counter);
        return new Statistics(counter.vaults, counter.folders, counter.documents,
                counter.frontMatters, counter.sections, counter.texts, counter.queries,
                counter.codeBlocks);
    }

    public static Statistics countAll(Vault vault)
    {
        return count(vault, ALL);
    }

    public static Statistics countFoldersAndDocuments(Vault vault)
    {
        return count(vault, FOLDERS_AND_DOCUMENTS);
    }

    @Override
    public void visit(Vault vault)
    {
        vaults++;
        super.visit(vault);
    }

    @Override
    public void visit(Folder folder)
    {
        folders++;
        super.visit(folder);
    }

    @Override
    public void visit(Document document)
    {
        documents++;
        if (scope == ALL)
        {
            super.visit(document);
        }
    }

    @Override
    public void visit(FrontMatter frontMatter)
    {
        frontMatters++;
    }

    @Override
    public void visit(Section section)
    {
        sections++;
        super.visit(section);
    }

    @Override
    public void visit(CodeBlock codeBlock)
    {
        codeBlocks++;
    }

    @Override
    public void visit(QueryBlock queryBlock)
    {
        queries++;
    }

    @Override
    public void visit(TextBlock textBlock)
    {
        texts++;
    }
}
