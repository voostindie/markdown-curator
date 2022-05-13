package nl.ulso.markdown_curator;

import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class ApplicationTest
{
    @Test
    void testVersion()
    {
        assertThat(Application.resolveVersion()).isNotEqualTo(Application.UNKNOWN_VERSION);
    }
}
