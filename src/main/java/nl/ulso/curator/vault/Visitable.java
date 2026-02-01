package nl.ulso.curator.vault;

public interface Visitable
{
    void accept(VaultVisitor visitor);
}
