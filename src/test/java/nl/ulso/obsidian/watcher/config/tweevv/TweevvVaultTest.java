package nl.ulso.obsidian.watcher.config.tweevv;

import nl.ulso.obsidian.watcher.config.personal.PersonalVault;
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
class TweevvVaultTest
{

    @InjectSoftAssertions
    private SoftAssertions softly;

    @Disabled
    @Test
    void constructVault()
            throws IOException
    {
        var vault = new TweevvVault();
        assertThat(vault.name()).endsWith("TweeVV");
        var counter = new ElementCounter();
        vault.accept(counter);
        System.out.println(counter);
    }
}
