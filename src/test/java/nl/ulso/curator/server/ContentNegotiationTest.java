package nl.ulso.curator.server;

import nl.ulso.curator.query.OutputFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static nl.ulso.curator.query.OutputFormat.JSON;
import static nl.ulso.curator.query.OutputFormat.MARKDOWN;
import static org.assertj.core.api.Assertions.assertThat;

class ContentNegotiationTest
{
    private static final OutputFormat CSV = new OutputFormat("csv", "text/csv");
    private static final OutputFormat MERMAID = new OutputFormat("mermaid", "text/plain");
    private static final OutputFormat PLAIN = new OutputFormat("plain", "text/plain");
    private static final List<OutputFormat> AVAILABLE =
        List.of(MARKDOWN, JSON, CSV, MERMAID, PLAIN);

    @Test
    void noPreferencesSelectsDefault()
    {
        assertThat(ContentNegotiation.none().select(AVAILABLE, JSON)).contains(JSON);
        assertThat(ContentNegotiation.of(null, null).select(AVAILABLE, JSON)).contains(JSON);
        assertThat(ContentNegotiation.of("  ", " ").select(AVAILABLE, JSON)).contains(JSON);
    }

    @Test
    void explicitFormatNameWins()
    {
        var negotiation = ContentNegotiation.of("application/json", "Mermaid");
        assertThat(negotiation.select(AVAILABLE, JSON)).contains(MERMAID);
    }

    @Test
    void unknownExplicitFormatSelectsNothing()
    {
        var negotiation = ContentNegotiation.of("*/*", "html");
        assertThat(negotiation.select(AVAILABLE, JSON)).isEmpty();
        assertThat(negotiation.describeFailure(List.of(JSON)))
            .isEqualTo("None of the requested output formats is available: 'html'. " +
                       "Available formats: json (application/json).");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "text/markdown                                      | markdown",
        "TEXT/MARKDOWN; charset=utf-8                       | markdown",
        "text/csv                                           | csv",
        "*/*                                                | json",
        "text/*                                             | csv",
        "text/html,application/xhtml+xml,*/*;q=0.8          | json",
        "application/json;q=0.5, text/markdown              | markdown",
        "text/markdown, application/json                    | markdown",
        "application/json, text/markdown                    | json",
        "text/*;q=0.9, text/csv;q=0.1, */*;q=0.5            | markdown",
        "text/plain                                         | mermaid",
        "image/png, text/csv;q=0.2                          | csv",
        "text/csv;q=0, */*                                  | json",
        "text/csv;q=abc, text/markdown;q=0.1                | markdown",
        "garbage, text/csv                                  | csv",
        "text/markdown;p=\"x,y;z\";q=0.1, application/json;q=0.5 | json",
    })
    void acceptHeaderSelection(String accept, String expected)
    {
        var selected = ContentNegotiation.of(accept, null).select(AVAILABLE, JSON);
        assertThat(selected).map(OutputFormat::name).contains(expected);
    }

    @Test
    void zeroQualityExcludesFormat()
    {
        var negotiation = ContentNegotiation.of("text/csv;q=0", null);
        assertThat(negotiation.select(AVAILABLE, JSON)).isEmpty();
    }

    @Test
    void unmatchedAcceptHeaderSelectsNothing()
    {
        var negotiation = ContentNegotiation.of("image/png", null);
        assertThat(negotiation.select(AVAILABLE, JSON)).isEmpty();
        assertThat(negotiation.describeFailure(List.of(MARKDOWN)))
            .isEqualTo("None of the requested output formats is available: image/png. " +
                       "Available formats: markdown (text/markdown).");
    }

    @Test
    void defaultFormatThatIsNotAvailableIsNotSelected()
    {
        assertThat(ContentNegotiation.none().select(List.of(MARKDOWN, CSV), JSON))
            .contains(CSV);
        assertThat(ContentNegotiation.of("*/*", null).select(List.of(MARKDOWN, CSV), JSON))
            .contains(CSV);
    }

    @Test
    void selectionIsAgainstTheFormatsAvailableAtThatMoment()
    {
        var negotiation = ContentNegotiation.of("text/csv", null);
        assertThat(negotiation.select(List.of(JSON, MARKDOWN), JSON)).isEmpty();
        assertThat(negotiation.select(List.of(JSON, MARKDOWN, CSV), JSON)).contains(CSV);
    }
}
