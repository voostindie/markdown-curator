package nl.ulso.markdown_curator.vault;

public interface Visitable
{
    void accept(VaultVisitor visitor);
}
