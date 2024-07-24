package nl.ulso.markdown_curator;

import java.nio.file.Path;

import static java.lang.System.getProperty;

/**
 * Helper methods to create paths to vaults in various locations.
 */
public final class VaultPaths
{
    private static final String USER_HOME_SYSTEM_PROPERTY = "user.home";

    private VaultPaths()
    {
    }

    public static Path pathInUserHome(String... path)
    {
        return Path.of(getProperty(USER_HOME_SYSTEM_PROPERTY), path);
    }

    public static Path iCloudObsidianVault(String vaultName)
    {
        return iCloudPath("iCloud~md~obsidian", vaultName);
    }

    public static Path iCloudIAWriterFolder(String folderName)
    {
        return iCloudPath("27N4MQEA55~pro~writer", folderName);
    }

    private static Path iCloudPath(String applicationName, String folderName)
    {
        return Path.of(getProperty(USER_HOME_SYSTEM_PROPERTY), "Library", "Mobile Documents",
                applicationName, "Documents", folderName);
    }
}
