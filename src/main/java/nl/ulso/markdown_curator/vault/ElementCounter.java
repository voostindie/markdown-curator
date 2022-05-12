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

    public static final class Statistics
    {
        public int vaults;
        public int folders;
        public int documents;
        public int frontMatters;
        public int sections;
        public int texts;
        public int queries;
        public int codeBlocks;

        @Override
        public String toString()
        {
            return "vaults: " + vaults + System.lineSeparator()
                    + "folders: " + folders + System.lineSeparator()
                    + "documents: " + documents + System.lineSeparator()
                    + "front matters: " + frontMatters + System.lineSeparator()
                    + "sections: " + sections + System.lineSeparator()
                    + "queries: " + queries + System.lineSeparator()
                    + "code blocks: " + codeBlocks + System.lineSeparator()
                    + "texts: " + texts;
        }
    }

    private final Scope scope;
    private final Statistics statistics;

    ElementCounter(Scope scope)
    {
        this.scope = scope;
        this.statistics = new Statistics();
    }

    private static Statistics count(Vault vault, Scope scope)
    {
        var counter = new ElementCounter(scope);
        vault.accept(counter);
        return counter.statistics;
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
        statistics.vaults++;
        super.visit(vault);
    }

    @Override
    public void visit(Folder folder)
    {
        statistics.folders++;
        super.visit(folder);
    }

    @Override
    public void visit(Document document)
    {
        statistics.documents++;
        if (scope == ALL)
        {
            super.visit(document);
        }
    }

    @Override
    public void visit(FrontMatter frontMatter)
    {
        statistics.frontMatters++;
    }

    @Override
    public void visit(Section section)
    {
        statistics.sections++;
        super.visit(section);
    }

    @Override
    public void visit(CodeBlock codeBlock)
    {
        statistics.codeBlocks++;
    }

    @Override
    public void visit(QueryBlock queryBlock)
    {
        statistics.queries++;
    }

    @Override
    public void visit(TextBlock textBlock)
    {
        statistics.texts++;
    }
}
