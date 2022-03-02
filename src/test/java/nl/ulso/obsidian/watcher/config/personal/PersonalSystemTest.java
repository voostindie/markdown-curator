package nl.ulso.obsidian.watcher.config.personal;

import nl.ulso.obsidian.watcher.vault.ElementCounter;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import static nl.ulso.obsidian.watcher.vault.ElementCounter.countAll;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class PersonalSystemTest
{

    @InjectSoftAssertions
    private SoftAssertions softly;

    @Disabled
    @Test
    void constructSystem()
            throws IOException
    {
        var vault = new PersonalSystem().vault();
        assertThat(vault.name()).endsWith("Personal");
        var statistics = countAll(vault);
        System.out.println(statistics);
    }
}
