package nl.ulso.obsidian.watcher.vault;

import com.google.common.jimfs.*;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static nl.ulso.obsidian.watcher.vault.ElementCounter.countAll;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@ExtendWith(SoftAssertionsExtension.class)
class FileSystemVaultTest
{
    private static final int POLLING_INTERVAL_MILLISECONDS = 10;
    private static final String ROOT = "/vault";

    @InjectSoftAssertions
    private SoftAssertions softly;

    private Path testVaultRoot;
    private FileSystemVault vault;

    @BeforeEach
    void setUpInMemoryFileSystem()
            throws IOException
    {
        var configuration = Configuration.unix().toBuilder()
                .setWatchServiceConfiguration(
                        WatchServiceConfiguration.polling(
                                POLLING_INTERVAL_MILLISECONDS,
                                TimeUnit.MILLISECONDS))
                .build();
        var fileSystem = Jimfs.newFileSystem(configuration);
        testVaultRoot = fileSystem.getPath(ROOT);
        writeFile("README.md", """
                ---
                aliases: [Index, Home]
                ---
                This is a dummy vault, for testing purposes.
                """);
        writeFile(".obsidian/hidden", "");
        writeFile("Movies/No Time to Die.md", "## Year\n\n2021");
        writeFile("Movies/Spectre.md", "## Year\n\n2015");
        writeFile("Movies/Skyfall.md", "## Year\n\n2012");
        writeFile("Movies/Quantum of Solace.md", "## Year\n\n2008");
        writeFile("Movies/Casino Royale.md", "## Year\n\n2006");
        writeFile("Characters/James Bond.md", "");
        writeFile("Characters/M.md", "");
        writeFile("Characters/Moneypenny.md", "");
        writeFile("Actors/Daniel Craig.md", "");
        writeFile("Actors/Ralph Fiennes.md", "");
        writeFile("Actors/Naomie Harris.md", "");
        writeFile("Actors/Judi Dench.md", "");
        vault = new FileSystemVault(testVaultRoot);
    }

    @Test
    void visit()
    {
        var statistics = countAll(vault);
        softly.assertThat(statistics.vaults).isEqualTo(1);
        softly.assertThat(statistics.folders).isEqualTo(3);
        softly.assertThat(statistics.documents).isEqualTo(13);
        softly.assertThat(statistics.frontMatters).isEqualTo(13);
        softly.assertThat(statistics.sections).isEqualTo(5);
        softly.assertThat(statistics.texts).isEqualTo(6);
    }

    @Test
    void testVaultContents()
    {
        softly.assertThat(vault.name()).endsWith("vault");
        softly.assertThat(vault.documents().size()).isEqualTo(1);
        softly.assertThat(vault.folders().size()).isEqualTo(3);
        softly.assertThat(vault.folder("Movies").get().documents().size()).isEqualTo(5);
        softly.assertThat(vault.folder("Characters").get().documents().size()).isEqualTo(3);
        softly.assertThat(vault.folder("Actors").get().documents().size()).isEqualTo(4);
    }

    @Test
    void readmeIsAvailable()
    {
        var document = vault.document("README").orElseThrow();
        assertThat(document.frontMatter().listOfStrings("aliases"))
                .containsExactly("Index", "Home");
    }

    @Test
    void watchVaultForNewFiles()
    {
        whileWatchingForChanges(new TestCase()
        {
            @Override
            public void changeFileSystem(FileSystem fileSystem)
                    throws IOException
            {
                writeFile("Actors/Christopher Waltz.md", "");
                writeFile("Characters/Blofeld.md", "");
            }

            @Override
            public void verify()
            {
                var characters = vault.folder("Characters").orElseThrow();
                softly.assertThat(characters.documents().size()).isEqualTo(4);
                softly.assertThat(characters.document("Blofeld")).isPresent();
                var actors = vault.folder("Actors").orElseThrow();
                softly.assertThat(actors.documents().size()).isEqualTo(5);
                softly.assertThat(actors.document("Christopher Waltz")).isPresent();
            }
        });
    }

    @Test
    void watchVaultForFilesInNewFolders()
    {
        whileWatchingForChanges(new TestCase()
        {
            @Override
            public void changeFileSystem(FileSystem fileSystem)
                    throws IOException
            {
                writeFile("Studios/MGM.md", "Metro-Goldwyn-Mayer");
            }

            @Override
            public void verify()
            {
                assertThat(vault.folder("Studios").get().document("MGM")).isPresent();
            }
        });
    }

    @Test
    void watchVaultForRenamedFolders()
    {
        whileWatchingForChanges(new TestCase()
        {
            @Override
            public void changeFileSystem(FileSystem fileSystem)
                    throws IOException
            {
                var oldPath = fileSystem.getPath(ROOT + "/Actors");
                var newPath = fileSystem.getPath(ROOT + "/People");
                Files.move(oldPath, newPath);
            }

            @Override
            public void verify()
            {
                softly.assertThat(vault.folder("Actors")).isNotPresent();
                softly.assertThat(vault.folder("People")).isPresent();
                softly.assertThat(vault.folder("People").get().documents().size()).isEqualTo(4);
            }
        });
    }

    @Test
    void watchVaultForDeletedDocument()
    {
        whileWatchingForChanges(new TestCase()
        {
            @Override
            public void changeFileSystem(FileSystem fileSystem)
                    throws IOException
            {
                Files.delete(fileSystem.getPath(ROOT + "/Characters/M.md"));
            }

            @Override
            public void verify()
            {
                softly.assertThat(vault.folder("Characters").get().documents().size()).isEqualTo(2);
                softly.assertThat(vault.folder("Characters").get().document("M")).isNotPresent();
            }
        });
    }

    @Test
    void watchVaultForChangedDocument()
    {
        whileWatchingForChanges(new TestCase()
        {
            private Document m;

            @Override
            public void changeFileSystem(FileSystem fileSystem)
                    throws IOException
            {
                m = vault.folder("Characters").orElseThrow().document("M").orElseThrow();
                writeFile("Characters/M.md", "Played by several actors");
            }

            @Override
            public void verify()
            {
                var newM = vault.folder("Characters").orElseThrow().document("M").orElseThrow();
                softly.assertThat(newM).isNotSameAs(m);
                softly.assertThat(newM.content()).isEqualTo("Played by several actors");
            }
        });
    }

    void whileWatchingForChanges(TestCase testCase)
    {
        var background = new Thread(() -> {
            try
            {
                vault.watchForChanges();
            }
            catch (InterruptedException | IOException e)
            {
                // Do nothing; there's nothing to clean up.
            }
        });
        background.start();
        try
        {
            testCase.changeFileSystem(testVaultRoot.getFileSystem());
            TimeUnit.MILLISECONDS.sleep(POLLING_INTERVAL_MILLISECONDS * 2);
        }
        catch (IOException | InterruptedException e)
        {
            fail("Unexpected exception in test", e);
        }
        testCase.verify();
        background.interrupt();
    }

    private void writeFile(String relativePath, String content)
            throws IOException
    {
        var fileSystem = testVaultRoot.getFileSystem();
        var absolutePath = fileSystem.getPath(ROOT + "/" + relativePath);
        Files.createDirectories(absolutePath.getParent());
        Files.write(absolutePath,
                List.of(content.split(System.lineSeparator())), StandardCharsets.UTF_8);
    }

    public interface TestCase
    {
        void changeFileSystem(FileSystem fileSystem)
                throws IOException;

        void verify();
    }
}