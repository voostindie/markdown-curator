package nl.ulso.markdown_curator.query.builtin;

import nl.ulso.markdown_curator.query.InMemoryQueryCatalog;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static nl.ulso.markdown_curator.vault.QueryBlockTest.emptyQueryBlock;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class HelpQueryTest
{

    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void helpQuery()
    {
        var catalog = new InMemoryQueryCatalog();
        var result = catalog.query("help").run(emptyQueryBlock()).toMarkdown();
        assertThat(result)
                .contains("### help")
                .contains("Shows detailed help information for a query.")
                .contains("Configuration options")
                .contains("- **name**: Name of the query to get help for.");
    }
}
