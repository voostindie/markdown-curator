package nl.ulso.macu.system.tweevv;

import nl.ulso.macu.vault.ElementCounter;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class TweevvTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Disabled
    @Test
    void constructSystem()
            throws IOException
    {
        var vault = new Tweevv().vault();
        Assertions.assertThat(vault.name()).endsWith("TweeVV");
        var statistics = ElementCounter.countAll(vault);
        System.out.println(statistics);
    }
}
