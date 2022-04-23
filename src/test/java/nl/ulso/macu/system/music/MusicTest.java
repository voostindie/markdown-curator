package nl.ulso.macu.system.music;

import nl.ulso.macu.query.*;
import nl.ulso.macu.vault.ElementCounter;
import nl.ulso.macu.vault.Dictionary;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class MusicTest
{
    private static Music music;

    @InjectSoftAssertions
    private SoftAssertions softly;

    @BeforeAll
    static void constructSystem()
            throws IOException
    {
        music = new Music();
    }

    @Test
    void statistics()
    {
        var vault = music.vault();
        assertThat(vault.name()).endsWith("music");
        var statistics = ElementCounter.countAll(vault);
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
        QueryCatalog catalog = music.queryCatalog();
        softly.assertThat(catalog.queries().size()).isEqualTo(3);
        Query dummy = catalog.query("dummy");
        QueryResult result = dummy.prepare(Dictionary.emptyDictionary()).run();
        softly.assertThat(result.isSuccess()).isFalse();
        softly.assertThat(result.errorMessage()).contains("no query defined called 'dummy'");
        softly.assertThat(result.errorMessage()).contains("albums");
        softly.assertThat(result.errorMessage()).contains("recordings");
        softly.assertThat(result.errorMessage()).contains("members");
    }

    @Test
    void queries()
    {
        var queries = music.vault().findAllQueries();
        softly.assertThat(queries.size()).isEqualTo(4);
    }
}
