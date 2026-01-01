package nl.ulso.markdown_curator.vault;

import nl.ulso.markdown_curator.Change;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.text.Normalizer.normalize;
import static java.util.Collections.synchronizedList;
import static nl.ulso.markdown_curator.Change.Kind.CREATION;
import static nl.ulso.markdown_curator.Change.Kind.DELETION;
import static nl.ulso.markdown_curator.vault.ElementCounter.countAll;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

@ExtendWith(SoftAssertionsExtension.class)
class FileSystemVaultTest
{
    private static final int WATCHER_INITIALIZATION_TIME_MILLISECONDS = 250;
    private static final int FILESYSTEM_WAIT_TIME_MILLISECONDS = 3000;

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
        softly.assertThat(vault.folder("Movies").orElseThrow().documents().size()).isEqualTo(5);
        softly.assertThat(vault.folder("Characters").orElseThrow().documents().size()).isEqualTo(3);
        softly.assertThat(vault.folder("Actors").orElseThrow().documents().size()).isEqualTo(4);
    }

    @Test
    void readmeIsAvailable()
    {
        var document = vault.document("README").orElseThrow();
        assertThat(document.frontMatter().listOfStrings("aliases")).containsExactly("Index",
                "Home");
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
                writeFile("Actors/Christopher Waltz.md", "Played [[Ernst Stavro Blofeld]]");
                return 2;
            }

            @Override
            public void verify(List<Change<?>> changes)
            {
                var characters = vault.folder("Characters").orElseThrow();
                softly.assertThat(characters.documents().size()).isEqualTo(4);
                softly.assertThat(characters.document("Blofeld")).isPresent();
                var actors = vault.folder("Actors").orElseThrow();
                softly.assertThat(actors.documents().size()).isEqualTo(5);
                softly.assertThat(actors.document("Christopher Waltz")).isPresent();
                softly.assertThat(changes.stream().map(Change::kind))
                        .containsExactly(CREATION, CREATION);
            }
        });
    }

    @Test
    @Disabled("Unpredictable; don't know why. Use only when needed. Or fix.")
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
            public void verify(List<Change<?>> changes)
            {
                softly.assertThat(vault.folder("Studios").orElseThrow().document("MGM"))
                        .isPresent();
                softly.assertThat(changes.stream()
                        .map(Change::objectType))
                        .map(c ->(Class) c)
                        .containsExactly(Folder.class, Document.class);
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
            public void verify(List<Change<?>> changes)
            {
                softly.assertThat(vault.folder("Actors")).isNotPresent();
                softly.assertThat(vault.folder("People")).isPresent();
                softly.assertThat(vault.folder("People").orElseThrow().documents().size())
                        .isEqualTo(4);
                softly.assertThat(changes.stream().map(Change::kind))
                        .contains(DELETION, CREATION);
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
            public void verify(List<Change<?>> changes)
            {
                softly.assertThat(vault.folder("Characters").orElseThrow().documents().size())
                        .isEqualTo(2);
                softly.assertThat(vault.folder("Characters").orElseThrow().document("M"))
                        .isNotPresent();
                softly.assertThat(changes.stream().map(Change::kind))
                        .containsExactly(DELETION);
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
            public void verify(List<Change<?>> changes)
            {
                var newM = vault.folder("Characters").orElseThrow().document("M").orElseThrow();
                softly.assertThat(newM).isNotSameAs(m);
                var textBlock = (TextBlock) newM.fragment(1);
                softly.assertThat(textBlock.markdown()).isEqualTo("Played by several actors\n");
            }
        });
    }

    /**
     * So, this was an evening of bug hunting... "All of a sudden" - which I know is never true when
     * it comes to computers; something changed, but I can't figure out what - documents with
     * diacritic characters in their names started to show up twice, leading to all kinds of issues,
     * especially since the system expects document names to be globally unique.
     * <p/>
     * After doing some digging I discovered diacritic characters were represented by slightly
     * different byte arrays. Doing some more digging I discovered this is a Unicade feature, where
     * text can be represented in either a "composed" or a "decomposed" manner. That means that
     * <code>"ë".contentEquals("ë")</code> might return <code>false</code>.
     * <p/>
     * The solution is to "normalize" the text to a specific form, where "NFC" is the W3C suggested
     * preferred form. This is what the FileSystemVault now does when resolving folder and document
     * names.
     * <p/>
     * Unfortunately I cannot test folders with diacritic characters, because writing folders with
     * diacritic characters in this test, in a temporary directory, somehow doesn't work. Files do
     * work, and this test indeed fails if document names are not normalized.
     */
    @Test
    @Disabled("Unpredictable; don't know why. Use only when needed. Or fix.")
    void handleUnicodeConsistently()
    {
        whileWatchingForChanges(new TestCase()
        {
            @Override
            public int changeFileSystem(FileSystem fileSystem)
                    throws IOException
            {
                writeFile(normalize("Unicode/éë - NFC.md", Normalizer.Form.NFC), "NFC");
                writeFile(normalize("Unicode/éë - NFD.md", Normalizer.Form.NFD), "NFD");
                writeFile(normalize("Unicode/éë - NFKC.md", Normalizer.Form.NFKC), "NFKC");
                writeFile(normalize("Unicode/éë - NFKD.md", Normalizer.Form.NFKD), "NFKD");
                return 4;
            }

            @Override
            public void verify(List<Change<?>> changes)
            {
                var expectedDocumentPrefix = normalize("éë - ", Normalizer.Form.NFC);
                var folder = vault.folder("Unicode").orElseThrow();
                softly.assertThat(folder.document(expectedDocumentPrefix + "NFC")).isPresent();
                softly.assertThat(folder.document(expectedDocumentPrefix + "NFD")).isPresent();
                softly.assertThat(folder.document(expectedDocumentPrefix + "NFKC")).isPresent();
                softly.assertThat(folder.document(expectedDocumentPrefix + "NFKD")).isPresent();
            }
        });
    }

    void whileWatchingForChanges(TestCase testCase)
    {
        var events = synchronizedList(new ArrayList<Change<?>>());
        vault.setVaultChangedCallback(events::add);

        var background = Thread.ofVirtual().factory().newThread(() -> vault.watchForChanges());
        background.start();
        try
        {
            // Needed because of https://github.com/gmethvin/directory-watcher/issues/87
            // We must give the watcher time to finish initializing, or else we'll miss events
            TimeUnit.MILLISECONDS.sleep(WATCHER_INITIALIZATION_TIME_MILLISECONDS);
        }
        catch (InterruptedException _)
        {
            Thread.currentThread().interrupt();
        }
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
        Files.write(absolutePath, List.of(content.split(System.lineSeparator())),
                StandardCharsets.UTF_8);
    }

    public interface TestCase
    {
        int changeFileSystem(FileSystem fileSystem)
                throws IOException;

        void verify(List<Change<?>> changes);
    }
}