package nl.ulso.curator.vault;

import java.util.Optional;

public class DocumentFinder
        extends BreadthFirstVaultVisitor
{
    private final String documentName;
    private Document document;

    public DocumentFinder(String documentName)
    {
        this.documentName = documentName;
        this.document = null;
    }

    @Override
    public void visit(Vault vault)
    {
        visit((Folder) vault);
    }

    @Override
    public void visit(Folder folder)
    {
        folder.document(documentName).ifPresentOrElse(
                d -> this.document = d,
                () -> {
                    for (var subfolder : folder.folders())
                    {
                        if (this.document != null)
                        {
                            break;
                        }
                        subfolder.accept(this);
                    }
                }
        );
    }

    public Optional<Document> document()
    {
        return Optional.ofNullable(document);
    }
}
