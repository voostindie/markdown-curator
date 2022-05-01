package nl.ulso.macu.query;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static nl.ulso.macu.vault.QueryBlockTest.emptyQueryBlock;
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
        assertThat(result).contains("**help**: shows detailed help information for a query.");
        assertThat(result).contains("Configuration options");
        assertThat(result).contains("- **query**: Name of the query to get help for.");
    }
}
