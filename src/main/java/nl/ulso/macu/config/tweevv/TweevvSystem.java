package nl.ulso.macu.config.tweevv;

import nl.ulso.macu.System;
import nl.ulso.macu.vault.FileSystemVault;
import nl.ulso.macu.vault.Vault;

import java.io.IOException;
import java.nio.file.Path;

public class TweevvSystem
        implements System
{
    private final Vault vault;

    public TweevvSystem()
            throws IOException
    {
        this.vault = new FileSystemVault(Path.of("/Users", "vincent", "Notes", "TweeVV"));
    }

    @Override
    public Vault vault()
    {
        return vault;
    }
}
