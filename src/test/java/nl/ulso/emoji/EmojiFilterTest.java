package nl.ulso.emoji;

import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static nl.ulso.emoji.EmojiFilter.stripEmojis;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class EmojiFilterTest
{
    @Test
    void emptyString()
    {
        assertThat(stripEmojis("")).isEqualTo("");
    }

    @Test
    void plainString()
    {
        assertThat(stripEmojis("Nothing special 123")).isEqualTo("Nothing special 123");
    }

    @Test
    void emojiString()
    {
        assertThat(stripEmojis("😱 Oh no! ⛔️")).isEqualTo(" Oh no! ");
    }
}
