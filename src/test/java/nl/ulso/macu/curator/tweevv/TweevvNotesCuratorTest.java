package nl.ulso.macu.curator.tweevv;

import nl.ulso.macu.vault.ElementCounter;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
@Tag("integration-test")
class TweevvNotesCuratorTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Disabled
    @Test
    void constructCurator()
    {
        var vault = new TweevvNotesCurator().vault();
        Assertions.assertThat(vault.name()).endsWith("TweeVV");
        var statistics = ElementCounter.countAll(vault);
        System.out.println(statistics);
    }
}
