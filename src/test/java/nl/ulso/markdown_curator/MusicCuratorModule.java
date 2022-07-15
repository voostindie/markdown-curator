package nl.ulso.markdown_curator;

import com.google.inject.Provides;

import java.io.IOException;
import java.nio.file.*;

/**
 * Module for testing. All testing is done in memory. All files in the "music" directory from this
 * project are copied into a memory-based file system first, after which a vault is initialized
 * on top of this file system. This ensures that the real file system stays intact. The vault on
 * disk is there to make it easy to maintain and use: just fire up Obsidian on top of it.
 */
public class MusicCuratorModule
        extends InMemoryCuratorModule
{
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
        registerQuery(AlbumsQuery.class);
        registerQuery(MembersQuery.class);
        registerQuery(RecordingsQuery.class);
    }
}
