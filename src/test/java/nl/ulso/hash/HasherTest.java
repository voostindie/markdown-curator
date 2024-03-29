package nl.ulso.hash;

import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static nl.ulso.hash.Hasher.hash;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(SoftAssertionsExtension.class)
class HasherTest
{
    @Test
    void nullString()
    {
        assertThatThrownBy(() -> hash(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void emptyString()
    {
        var hash = hash("");
        assertThat(hash).isEqualTo("e3b0c442");
    }

    @Test
    void string()
    {
        var hash = hash("Vincent Oostindie");
        assertThat(hash).isEqualTo("e32a0c07");
    }
}
