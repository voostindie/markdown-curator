package nl.ulso.macu.vault;

/**
 * This is an extremely simply callback that is triggered whenever the vault is changed.
 * <p/>
 * It doesn't say what has changed, nor does it make any promises on how often it is called when
 * something changes. This is the simplest of callbacks. It's probably the first place to look for
 * optimizations when these are needed.
 */
public interface VaultChangedCallback
{
    void vaultChanged();
}
