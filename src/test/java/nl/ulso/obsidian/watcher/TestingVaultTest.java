package nl.ulso.obsidian.watcher;

import nl.ulso.obsidian.watcher.config.personal.PersonalVault;
import nl.ulso.obsidian.watcher.config.rabobank.RabobankVault;
import nl.ulso.obsidian.watcher.vault.ElementCounter;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class TestingVaultTest
{
    @Test
    void constructVault()
            throws IOException
    {
        var vault = new TestingVault();
        assertThat(vault.name()).endsWith("src/test/resources/vault");
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