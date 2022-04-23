package nl.ulso.macu.query;

import nl.ulso.macu.vault.Dictionary;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
class InMemoryQueryCatalogTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void emptyCatalog()
    {
        var catalog = new InMemoryQueryCatalog(null);
        var specification = catalog.query("invalid");
        var query = specification.prepare(Dictionary.emptyDictionary());
        var result = query.run();
        softly.assertThat(result.isSuccess()).isFalse();
        softly.assertThat(result.columns()).isEmpty();
        softly.assertThat(result.rows()).isEmpty();
        softly.assertThat(result.errorMessage()).contains("no queries defined");
    }
}
