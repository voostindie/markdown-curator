package nl.ulso.macu.vault;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.List.copyOf;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * Represents a pointer to a document in the vault, and optionally to a specific section within
 * that document.
 */
public record Location(List<Folder> vaultPath, Document document, List<Section> documentPath)
{
    public Location(List<Folder> vaultPath, Document document, List<Section> documentPath)
    {
        if (vaultPath == null)
        {
            this.vaultPath = emptyList();
        }
        else
        {
            this.vaultPath = copyOf(vaultPath);
        }
        this.document = requireNonNull(document);
        this.documentPath = documentPath != null ? copyOf(documentPath) : emptyList();
    }

    @Override
    public String toString()
    {
        var builder = new StringBuilder();
        builder.append(vaultPath.stream().map(Folder::name).collect(joining("/")));
        if (!vaultPath.isEmpty())
        {
            builder.append("/");
        }
        builder.append(document.name());
        if (!documentPath.isEmpty())
        {
            builder.append("#");
            builder.append(documentPath.stream().map(Section::anchor).collect(joining("&")));
        }
        return builder.toString();
    }
}
