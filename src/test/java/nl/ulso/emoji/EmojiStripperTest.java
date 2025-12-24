package nl.ulso.emoji;

import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.stream.Stream;

import static nl.ulso.emoji.EmojiStripper.stripEmojisFrom;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class EmojiStripperTest
{
    @Test
    void emptyString()
    {
        assertThat(stripEmojisFrom("")).isEmpty();
    }

    @Test
    void plainString()
    {
        assertThat(stripEmojisFrom("Nothing special 123")).isEqualTo("Nothing special 123");
    }

    @Test
    void emojiString()
    {
        assertThat(stripEmojisFrom("😱 Oh no! ⛔️")).isEqualTo(" Oh no! ");
    }

    @Test
    void keepPunctuation()
    {
        assertThat(stripEmojisFrom("{[.!,?]}")).isEqualTo("{[.!,?]}");
    }

    @Test
    void asFunction()
    {
        var stripFunction = new EmojiStripper();
        var list = Stream.of("", "😱 Oh no!", "🦦 in the stream")
            .map(stripFunction)
            .toList();
        assertThat(list).containsExactly("", " Oh no!", " in the stream");

    }
}
