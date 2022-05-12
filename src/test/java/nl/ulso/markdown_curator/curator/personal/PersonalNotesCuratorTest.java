package nl.ulso.markdown_curator.curator.personal;

import nl.ulso.markdown_curator.vault.ElementCounter;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
@Tag("integration-test")
class PersonalNotesCuratorTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Disabled
    @Test
    void constructCurator()
    {
        var vault = new PersonalNotesCurator().vault();
        assertThat(vault.name()).endsWith("Personal");
        var statistics = ElementCounter.countAll(vault);
        System.out.println(statistics);
    }
}
