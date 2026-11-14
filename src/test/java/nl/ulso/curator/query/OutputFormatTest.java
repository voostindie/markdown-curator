package nl.ulso.curator.query;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static nl.ulso.curator.query.OutputFormat.JSON;
import static nl.ulso.curator.query.OutputFormat.MARKDOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutputFormatTest
{
    @Test
    void builtInFormats()
    {
        assertThat(MARKDOWN.inline()).isTrue();
        assertThat(JSON.inline()).isFalse();
    }

    @Test
    void nameAndMimeTypeAreNormalized()
    {
        var format = new OutputFormat(" CSV ", "Text/CSV");
        assertThat(format.name()).isEqualTo("csv");
        assertThat(format.mimeType()).isEqualTo("text/csv");
        assertThat(format).isEqualTo(new OutputFormat("csv", "text/csv"));
    }

    @Test
    void formatsAreFencedByDefault()
    {
        assertThat(new OutputFormat("csv", "text/csv").inline()).isFalse();
        assertThat(new OutputFormat("csv", "text/csv", true).inline()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "my format", "-csv", "csv`", "c\nsv"})
    void invalidNamesAreRejected(String name)
    {
        assertThatThrownBy(() -> new OutputFormat(name, "text/csv"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "text", "/csv", "text/", "text/csv/x", "*/*", "text/*",
        "text/csv; charset=utf-8", "text/ csv"})
    void invalidMimeTypesAreRejected(String mimeType)
    {
        assertThatThrownBy(() -> new OutputFormat("csv", mimeType))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullsAreRejected()
    {
        assertThatThrownBy(() -> new OutputFormat(null, "text/csv"))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OutputFormat("csv", null))
            .isInstanceOf(NullPointerException.class);
    }
}
