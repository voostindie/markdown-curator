package nl.ulso.curator.addon.omnifocus;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.File;
import java.nio.file.Path;

@Singleton
final class DefaultOmniFocusDatabase
    implements OmniFocusDatabase
{
    private final File databaseFile;

    @Inject
    DefaultOmniFocusDatabase()
    {
        this(Path.of(
            System.getProperty("user.home"),
            "Library",
            "Containers",
            "com.omnigroup.OmniFocus4",
            "Data",
            "Library",
            "Application Support",
            "OmniFocus",
            "OmniFocus.ofocus"
        ).toFile());
    }

    // For testing purposes
    DefaultOmniFocusDatabase(File databaseFile)
    {
        this.databaseFile = databaseFile;
    }

    @Override
    public boolean isAccessible()
    {
        return databaseFile.canRead();
    }

    @Override
    public String path()
    {
        return databaseFile.getAbsolutePath();
    }

    @Override
    public long lastModified()
    {
        return databaseFile.lastModified();
    }
}
