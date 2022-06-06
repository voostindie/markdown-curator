package nl.ulso.markdown_curator;

import nl.ulso.markdown_curator.query.QueryCatalog;
import nl.ulso.markdown_curator.vault.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;

/**
 * System for testing. All testing is done in memory. All files in the "music" directory from this
 * project are copied into a memory-based file system first, after which a {@link FileSystemVault}
 * is initialized on top of this file system. This ensures that the file system stays intact. The
 * vault on disk is there to make it easy to maintain and use: just fire up Obsidian on top of it.
 * <p/>
 * This class is in the same package as the {@link CuratorTemplate} base class, so that its
 * package-local methods are available for testing.
 */
public class MusicCurator
        extends InMemoryCuratorTemplate
{
    @Override
    protected FileSystemVault createVault()
            throws IOException
    {
        var sourceRoot = Paths.get("").toAbsolutePath()
                .resolve("src/test/resources/music");
        return copyVaultToMemory(sourceRoot, "/music");
    }

    @Override
    protected Set<DataModel> createDataModels(Vault vault)
    {
        return Collections.emptySet();
    }

    @Override
    protected void registerQueries(QueryCatalog catalog, Vault vault, DataModelMap dataModels)
    {
        catalog.register(new AlbumsQuery(vault));
        catalog.register(new RecordingsQuery(vault));
        catalog.register(new MembersQuery(vault));
    }

    String reload(Document document)
            throws IOException
    {
        var path = vault().resolveAbsolutePath(document);
        return Files.readString(path).trim();
    }
}
