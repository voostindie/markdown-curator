package nl.ulso.obsidian.watcher.vault;

import com.google.common.jimfs.*;
import nl.ulso.obsidian.watcher.config.personal.PersonalVault;
import nl.ulso.obsidian.watcher.config.rabobank.RabobankVault;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class FileSystemVaultTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    private TestVault vault;

    @BeforeAll
    static void setUpInMemoryFileSystem()
            throws IOException
    {
        var configuration = Configuration.unix().toBuilder()
                .setWatchServiceConfiguration(
                        WatchServiceConfiguration.polling(10, TimeUnit.MILLISECONDS))
                .build();
        var fileSystem = Jimfs.newFileSystem(configuration);
        addFile(fileSystem, "README.md", """
                ---
                aliases: [Index, Home]
                ---
                This is a dummy vault, for testing purposes.
                """);
        addFile(fileSystem, ".obsidian/hidden", "");
        FileSystemVault.setFileSystemForTesting(fileSystem);
    }

    @BeforeEach
    void constructTestVault()
            throws IOException
    {
        vault = new TestVault();
    }

    private static void addFile(FileSystem fileSystem, String relativePath, String content)
            throws IOException
    {
        var absolutePath = fileSystem.getPath(TestVault.ROOT + "/" + relativePath);
        Files.createDirectories(absolutePath.getParent());
        Files.write(absolutePath,
                List.of(content.split(System.lineSeparator())), StandardCharsets.UTF_8);
    }

    @Test
    void testVaultName()
    {
        assertThat(vault.name()).endsWith("vault");
    }

    @Test
    void testVaultContents()
    {
        softly.assertThat(vault.documents().size()).isEqualTo(1);
        softly.assertThat(vault.folders().size()).isEqualTo(0);
    }

    @Test
    void readmeIsAvailable()
    {
        var document = vault.document("README").orElseThrow();
        assertThat(document.frontMatter().listOfStrings("aliases"))
                .containsExactly("Index", "Home");
    }

    // Test is disabled by default on purpose!
    void rabobankVault()
            throws IOException
    {
        var vault = new RabobankVault();
        assertThat(vault.name()).endsWith("Rabobank");
        var counter = new ElementCounter();
        vault.accept(counter);
        System.out.println(counter);
    }

    // Test is disabled by default on purpose!
    void personalVault()
            throws IOException
    {
        var vault = new PersonalVault();
        assertThat(vault.name()).endsWith("Personal");
        var counter = new ElementCounter();
        vault.accept(counter);
        System.out.println(counter);
    }
}