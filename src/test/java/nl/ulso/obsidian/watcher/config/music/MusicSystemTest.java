package nl.ulso.obsidian.watcher.config.music;

import nl.ulso.obsidian.watcher.query.*;
import nl.ulso.obsidian.watcher.vault.Dictionary;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import static nl.ulso.obsidian.watcher.vault.ElementCounter.countAll;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class MusicSystemTest
{
    private static MusicSystem musicSystem;

    @InjectSoftAssertions
    private SoftAssertions softly;

    @BeforeAll
    static void constructSystem()
            throws IOException
    {
        musicSystem = new MusicSystem();
    }

    @Test
    void statistics()
    {
        var vault = musicSystem.vault();
        assertThat(vault.name()).endsWith("music");
        var statistics = countAll(vault);
        System.out.println(statistics);
        softly.assertThat(statistics.vaults).isEqualTo(1);
        softly.assertThat(statistics.folders).isEqualTo(3);
        softly.assertThat(statistics.documents).isEqualTo(11);
        softly.assertThat(statistics.frontMatters).isEqualTo(11);
        softly.assertThat(statistics.sections).isEqualTo(29);
        softly.assertThat(statistics.queries).isEqualTo(4);
        softly.assertThat(statistics.codeBlocks).isEqualTo(4);
        softly.assertThat(statistics.texts).isEqualTo(34);
    }

    @Test
    void queryCatalog()
    {
        QueryCatalog catalog = musicSystem.queryCatalog();
        softly.assertThat(catalog.specifications().size()).isEqualTo(3);
        QuerySpecification dummy = catalog.specificationFor("dummy");
        QueryResult result = dummy.configure(Dictionary.emptyDictionary()).run(musicSystem.vault());
        softly.assertThat(result.isValid()).isFalse();
        softly.assertThat(result.errorMessage()).contains("no query defined called 'dummy'");
        softly.assertThat(result.errorMessage()).contains("albums");
        softly.assertThat(result.errorMessage()).contains("recordings");
        softly.assertThat(result.errorMessage()).contains("members");
    }

    @Test
    void queries()
    {
        var queries = musicSystem.vault().findAllQueries();
        softly.assertThat(queries.size()).isEqualTo(4);
        queries.keySet().forEach((query) -> {
            var location = queries.get(query);
            softly.assertThat(location.vaultPath()).isNotEmpty();
            softly.assertThat(location.document()).isNotNull();
            softly.assertThat(location.documentPath()).isNotEmpty();
        });
    }
}
