package nl.ulso.obsidian.watcher.config.rabobank;

import nl.ulso.obsidian.watcher.vault.ElementCounter;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class RabobankVaultTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Disabled
    @Test
    void constructVault()
            throws IOException
    {
        var vault = new RabobankVault();
        assertThat(vault.name()).endsWith("Rabobank");
        var counter = new ElementCounter();
        vault.accept(counter);
        System.out.println(counter);
    }
}
