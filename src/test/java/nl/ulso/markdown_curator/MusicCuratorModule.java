package nl.ulso.markdown_curator;

import nl.ulso.markdown_curator.journal.JournalModule;
import nl.ulso.markdown_curator.links.LinksModule;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Module for testing. All testing is done in memory. All files in the "music" directory from this
 * project are copied into a memory-based file system first, after which a vault is initialized
 * on top of this file system. This ensures that the real file system stays intact. The vault on
 * disk is there to make it easy to maintain and use: just fire up Obsidian on top of it.
 */
public class MusicCuratorModule
        extends InMemoryCuratorModule
{
    private boolean configured = false;

    @Override
    public String name()
    {
        return "music";
    }

    @Override
    public Path vaultPath()
    {
        var sourceRoot = Paths.get("").toAbsolutePath()
                .resolve("src/test/resources/music");
        return copyVaultToMemory(sourceRoot, "/music");
    }

    @Override
    protected void configureCurator()
    {

        install(new LinksModule());
        install(new JournalModule("journal", "Log"));
        registerQuery(AlbumsQuery.class);
        registerQuery(MembersQuery.class);
        registerQuery(RecordingsQuery.class);
        registerQuery(NoOpQuery.class);
        configured = true;
    }

    public boolean isConfigured()
    {
        return configured;
    }
}
