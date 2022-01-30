package nl.ulso.obsidian.watcher;

import nl.ulso.obsidian.watcher.config.rabobank.RabobankVault;
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

    void rabobankVault() throws IOException
    {
        var vault = new RabobankVault();
        assertThat(vault.name()).endsWith("Rabobank");
    }
}