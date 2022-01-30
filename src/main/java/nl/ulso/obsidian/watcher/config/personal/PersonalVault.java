package nl.ulso.obsidian.watcher.config.personal;

import nl.ulso.obsidian.watcher.vault.FileSystemVault;

import java.io.IOException;
import java.nio.file.Path;

public class PersonalVault
        extends FileSystemVault
{
    public PersonalVault()
            throws IOException
    {
        super(Path.of("/Users", "vincent", "Notes", "Personal"));
    }
}
