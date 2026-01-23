package nl.ulso.markdown_curator.query.builtin;

import nl.ulso.markdown_curator.Change;
import nl.ulso.markdown_curator.Changelog;
import nl.ulso.markdown_curator.query.QueryDefinition;
import nl.ulso.markdown_curator.vault.*;

import java.util.function.Predicate;

import static nl.ulso.markdown_curator.Change.isObjectType;

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
        return isObjectType(Document.class).and(change ->
            isInHierarchyOf(
                vault,
                resolveFolderName(definition), change.as(Document.class).object().folder()
            )
        );
    }

    private Predicate<Change<?>> isFolderInHierarchy(QueryDefinition definition)
    {
        return isObjectType(Folder.class).and(change ->
            isInHierarchyOf(
                vault,
                resolveFolderName(definition), change.as(Folder.class).object()
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
