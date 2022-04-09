package nl.ulso.macu.vault;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class LocationTest
{

    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void equalsContract()
    {
        EqualsVerifier.forClass(Location.class)
                .withNonnullFields("vaultPath", "document", "documentPath").verify();
    }

}
