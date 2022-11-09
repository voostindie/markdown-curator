package nl.ulso.markdown_curator.vault;

import nl.ulso.markdown_curator.vault.event.*;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.synchronizedList;
import static nl.ulso.markdown_curator.vault.ElementCounter.countAll;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

@ExtendWith(SoftAssertionsExtension.class)
class FileSystemVaultTest
{
    private static final int FILESYSTEM_WAIT_TIME_MILLISECONDS = 1000; // "Works on my machine!"

    @InjectSoftAssertions
    private SoftAssertions softly;

    private Path testVaultRoot;
    private FileSystemVault vault;

    @BeforeEach
    void setUpFileSystemForTesting()
            throws IOException
    {
        testVaultRoot = Files.createTempDirectory("macu-");
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

    @AfterEach
    void deleteTemporaryFiles()
    {
        deleteRecursively(testVaultRoot.toFile());
    }

    private void deleteRecursively(File directory)
    {
        var files = directory.listFiles();
        if (files != null)
        {
            for (File file : files)
            {
                if (file.isDirectory())
                {
                    deleteRecursively(file);
                }
                else
                {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    @Test
    void visit()
    {
        var statistics = countAll(vault);
        softly.assertThat(statistics.vaults()).isEqualTo(1);
        softly.assertThat(statistics.folders()).isEqualTo(3);
        softly.assertThat(statistics.documents()).isEqualTo(13);
        softly.assertThat(statistics.frontMatters()).isEqualTo(13);
        softly.assertThat(statistics.sections()).isEqualTo(5);
        softly.assertThat(statistics.texts()).isEqualTo(13);
    }

    @Test
    void testVaultContents()
    {
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
            public int changeFileSystem(FileSystem fileSystem)
                    throws IOException
            {
                writeFile("Characters/Blofeld.md", "Ernst Stavro");
                writeFile("Actors/Christopher Waltz.md", "");
                return 2;
            }

            @Override
            public void verify(List<VaultChangedEvent> events)
            {
                var characters = vault.folder("Characters").orElseThrow();
                softly.assertThat(characters.documents().size()).isEqualTo(4);
                softly.assertThat(characters.document("Blofeld")).isPresent();
                var actors = vault.folder("Actors").orElseThrow();
                softly.assertThat(actors.documents().size()).isEqualTo(5);
                softly.assertThat(actors.document("Christopher Waltz")).isPresent();
                softly.assertThat(events.stream().map(e -> (Class) e.getClass()))
                        .containsExactly(DocumentAdded.class, DocumentAdded.class);
            }
        });
    }

    @Test
    void watchVaultForFilesInNewFolders()
    {
        whileWatchingForChanges(new TestCase()
        {
            @Override
            public int changeFileSystem(FileSystem fileSystem)
                    throws IOException
            {
                writeFile("Studios/MGM.md", "Metro-Goldwyn-Mayer");
                return 2;
            }

            @Override
            public void verify(List<VaultChangedEvent> events)
            {
                softly.assertThat(vault.folder("Studios").get().document("MGM")).isPresent();
                softly.assertThat(events.stream().map(e -> (Class) e.getClass()))
                        .containsExactly(FolderAdded.class, DocumentAdded.class);
            }
        });
    }

    @Test
    void watchVaultForRenamedFolders()
    {
        whileWatchingForChanges(new TestCase()
        {
            @Override
            public int changeFileSystem(FileSystem fileSystem)
                    throws IOException
            {
                var oldPath = testVaultRoot.resolve("Actors");
                var newPath = testVaultRoot.resolve("People");
                Files.move(oldPath, newPath);
                return 2;
            }

            @Override
            public void verify(List<VaultChangedEvent> events)
            {
                softly.assertThat(vault.folder("Actors")).isNotPresent();
                softly.assertThat(vault.folder("People")).isPresent();
                softly.assertThat(vault.folder("People").get().documents().size()).isEqualTo(4);
                softly.assertThat(events.stream().map(e -> (Class) e.getClass()))
                        .contains(FolderRemoved.class, FolderAdded.class);
            }
        });
    }

    @Test
    void watchVaultForDeletedDocument()
    {
        whileWatchingForChanges(new TestCase()
        {
            @Override
            public int changeFileSystem(FileSystem fileSystem)
                    throws IOException
            {
                Files.delete(testVaultRoot.resolve("Characters/M.md"));
                return 1;
            }

            @Override
            public void verify(List<VaultChangedEvent> events)
            {
                softly.assertThat(vault.folder("Characters").get().documents().size()).isEqualTo(2);
                softly.assertThat(vault.folder("Characters").get().document("M")).isNotPresent();
                softly.assertThat(events.stream().map(e -> (Class) e.getClass()))
                        .containsExactly(DocumentRemoved.class);
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
            public int changeFileSystem(FileSystem fileSystem)
                    throws IOException
            {
                m = vault.folder("Characters").orElseThrow().document("M").orElseThrow();
                writeFile("Characters/M.md", "Played by several actors");
                return 1;
            }

            @Override
            public void verify(List<VaultChangedEvent> events)
            {
                var newM = vault.folder("Characters").orElseThrow().document("M").orElseThrow();
                softly.assertThat(newM).isNotSameAs(m);
                softly.assertThat(newM.content()).isEqualTo("Played by several actors");
            }
        });
    }

    void whileWatchingForChanges(TestCase testCase)
    {
        var events = synchronizedList(new ArrayList<VaultChangedEvent>());
        vault.setVaultChangedCallback(events::add);
        var background = new Thread(() -> vault.watchForChanges());
        background.start();
        try
        {
            int expectedEventCount = testCase.changeFileSystem(testVaultRoot.getFileSystem());
            await().atMost(FILESYSTEM_WAIT_TIME_MILLISECONDS, TimeUnit.MILLISECONDS)
                    .until(() -> events.size() >= expectedEventCount);
        }
        catch (IOException e)
        {
            fail("Unexpected exception in test", e);
        }
        background.interrupt();
        testCase.verify(events);
    }

    private void writeFile(String relativePath, String content)
            throws IOException
    {
        var absolutePath = testVaultRoot.resolve(relativePath);
        Files.createDirectories(absolutePath.getParent());
        Files.write(absolutePath,
                List.of(content.split(System.lineSeparator())), StandardCharsets.UTF_8);
    }

    public interface TestCase
    {
        int changeFileSystem(FileSystem fileSystem)
                throws IOException;

        void verify(List<VaultChangedEvent> events);
    }
}