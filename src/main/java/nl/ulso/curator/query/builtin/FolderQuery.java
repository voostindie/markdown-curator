package nl.ulso.curator.query.builtin;

import nl.ulso.curator.Change;
import nl.ulso.curator.Changelog;
import nl.ulso.curator.query.QueryDefinition;
import nl.ulso.curator.vault.*;

import java.util.function.Predicate;

import static nl.ulso.curator.Change.isPayloadType;

abstract class FolderQuery
{
    private final Vault vault;

    public FolderQuery(Vault vault)
    {
        this.vault = vault;
    }

    public boolean isImpactedBy(Changelog changelog, QueryDefinition definition)
    {
        return changelog.changes()
            .anyMatch(isDocumentInHierarchy(definition).or(isFolderInHierarchy(definition)));
    }

    private Predicate<Change<?>> isDocumentInHierarchy(QueryDefinition definition)
    {
        return isPayloadType(Document.class).and(change ->
            isInHierarchyOf(
                vault,
                resolveFolderName(definition), change.as(Document.class).value().folder()
            )
        );
    }

    private Predicate<Change<?>> isFolderInHierarchy(QueryDefinition definition)
    {
        return isPayloadType(Folder.class).and(change ->
            isInHierarchyOf(
                vault,
                resolveFolderName(definition), change.as(Folder.class).value()
            )
        );
    }

    protected boolean isInHierarchyOf(Vault vault, String parentFolderName, Folder child)
    {
        var folder = child;
        while (!folder.name().contentEquals(parentFolderName))
        {
            if (folder == vault)
            {
                return false;
            }
            folder = folder.parent();
        }
        return true;
    }

    protected String resolveFolderName(QueryDefinition definition)
    {
        return definition.configuration().string("folder", definition.document().folder().name());
    }

    protected Vault vault()
    {
        return vault;
    }
}
