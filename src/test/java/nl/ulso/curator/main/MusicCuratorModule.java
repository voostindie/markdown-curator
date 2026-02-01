package nl.ulso.curator.main;

import dagger.Module;
import dagger.*;
import dagger.multibindings.IntoSet;
import nl.ulso.curator.CuratorModule;
import nl.ulso.curator.addon.journal.JournalModule;
import nl.ulso.curator.addon.journal.JournalSettings;
import nl.ulso.curator.addon.links.LinksModule;
import nl.ulso.curator.query.Query;

import java.nio.file.*;
import java.util.Locale;

import static java.util.Locale.ENGLISH;

/**
 * Module for testing. All testing is done in memory. All files in the "music" directory from this
 * project are copied into a memory-based file system first, after which a vault is initialized
 * on top of this file system. This ensures that the real file system stays intact. The vault on
 * disk is there to make it easy to maintain and use: just fire up Obsidian on top of it.
 */
@Module(includes = {CuratorModule.class, JournalModule.class, LinksModule.class})
abstract class MusicCuratorModule
        extends InMemoryCuratorModule
{
    @Provides
    static Path vaultPath()
    {
        var sourceRoot = Paths.get("").toAbsolutePath().resolve("src/test/resources/music");
        return copyVaultToMemory(sourceRoot, "/music");
    }

    @Provides
    static WatchService watchService(Path vaultPath)
    {
        return createWatchService(vaultPath);
    }

    @Provides
    static Locale locale()
    {
        return ENGLISH;
    }

    @Provides
    static JournalSettings journalSettings()
    {
        return new JournalSettings(
                "journal",
                "markers",
                "Log",
                "projects"
        );
    }

    @Binds
    @IntoSet
    abstract Query bindAlbumsQuery(AlbumsQuery albumsQuery);

    @Binds
    @IntoSet
    abstract Query bindMembersQuery(MembersQuery membersQuery);

    @Binds
    @IntoSet
    abstract Query bindRecordingsQuery(RecordingsQuery recordingsQuery);
}
