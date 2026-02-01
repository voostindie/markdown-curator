package nl.ulso.curator.vault;

import java.util.Collection;

public class VaultStub
        extends FolderStub
        implements Vault
{
    public VaultStub()
    {
        this("stub");
    }

    public VaultStub(String name)
    {
        super(null, name);
    }

    @Override
    public void accept(VaultVisitor visitor)
    {
        visitor.visit(this);
    }

    @Override
    public Collection<QueryBlock> findAllQueryBlocks()
    {
        var finder = new QueryBlockFinder();
        finder.visit(this);
        return finder.queries();
    }

    @Override
    public void setVaultChangedCallback(VaultChangedCallback callback)
    {
        // Do nothing
    }

    @Override
    public void watchForChanges()
    {
        // Do nothing
    }
}
