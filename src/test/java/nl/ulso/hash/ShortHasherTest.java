package nl.ulso.hash;

import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.stream.Stream;

import static nl.ulso.hash.ShortHasher.shortHashOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(SoftAssertionsExtension.class)
class ShortHasherTest
{
    @Test
    void nullString()
    {
        assertThatThrownBy(() -> shortHashOf(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void emptyString()
    {
        var hash = shortHashOf("");
        assertThat(hash).isEqualTo("e3b0c442");
    }

    @Test
    void string()
    {
        var hash = shortHashOf("Vincent Oostindie");
        assertThat(hash).isEqualTo("e32a0c07");
    }

    @Test
    void asFunction()
    {
        var hashFunction = new ShortHasher();
        var hashes = Stream.of("Vincent", "Oostindie")
            .map(hashFunction)
            .toList();
        assertThat(hashes).containsExactly("f44c4b07", "a5b6466c");
    }
}
