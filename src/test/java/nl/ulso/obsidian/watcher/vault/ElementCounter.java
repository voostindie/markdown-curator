package nl.ulso.obsidian.watcher.vault;

/**
 * Simple visitor that counts all occurrences of the different types
 */
public class ElementCounter
        implements Visitor
{
    int vaults = 0;
    int folders = 0;
    int documents = 0;
    int frontMatters = 0;
    int sections = 0;
    int texts = 0;

    @Override
    public void visitVault(Vault vault)
    {
        vaults++;
        vault.folders().forEach(f -> f.accept(this));
        vault.documents().forEach(d -> d.accept(this));
    }

    @Override
    public void visitFolder(Folder folder)
    {
        folders++;
        folder.folders().forEach(f -> f.accept(this));
        folder.documents().forEach(d -> d.accept(this));
    }

    @Override
    public void visitDocument(Document document)
    {
        documents++;
        document.fragments().forEach(f -> f.accept(this));
    }

    @Override
    public void visitFrontMatter(FrontMatter frontMatter)
    {
        frontMatters++;
    }

    @Override
    public void visitSection(Section section)
    {
        sections++;
        section.fragments().forEach(f -> f.accept(this));
    }

    @Override
    public void visitText(Text text)
    {
        texts++;
    }

    @Override
    public String toString()
    {
        return "vaults: " + vaults + System.lineSeparator() +
                "folders: " + folders + System.lineSeparator() +
                "documents: " + documents + System.lineSeparator() +
                "front matters: " + frontMatters + System.lineSeparator() +
                "sections: " + sections + System.lineSeparator() +
                "text: " + texts;
    }
}
