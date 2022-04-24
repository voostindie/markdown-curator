package nl.ulso.macu.query;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static nl.ulso.macu.vault.QueryBlockTest.emptyQueryBlock;

@ExtendWith(SoftAssertionsExtension.class)
class InMemoryQueryCatalogTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void emptyCatalog()
    {
        var catalog = new InMemoryQueryCatalog();
        var specification = catalog.query("invalid");
        var result = specification.run(emptyQueryBlock());
        softly.assertThat(result.isSuccess()).isFalse();
        softly.assertThat(result.columns()).isEmpty();
        softly.assertThat(result.rows()).isEmpty();
        softly.assertThat(result.errorMessage()).contains("no queries defined");
    }
}
