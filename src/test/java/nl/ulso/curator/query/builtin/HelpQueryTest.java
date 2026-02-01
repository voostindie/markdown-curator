package nl.ulso.curator.query.builtin;

import nl.ulso.curator.query.InMemoryQueryCatalog;
import nl.ulso.curator.query.QueryResultFactory;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static java.util.Collections.emptySet;
import static nl.ulso.curator.vault.QueryBlockTest.emptyQueryBlock;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class HelpQueryTest
{
    @Test
    void helpQuery()
    {
        var catalog = new InMemoryQueryCatalog(emptySet(), new QueryResultFactory());
        var result = catalog.query("help").run(emptyQueryBlock()).toMarkdown();
        assertThat(result)
                .contains("### help")
                .contains("Shows detailed help information for a query.")
                .contains("Configuration options")
                .contains("- **name**: Name of the query to get help for.");
    }
}
