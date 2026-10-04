package nl.ulso.curator.server;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static nl.ulso.curator.query.OutputFormat.JSON;
import static nl.ulso.curator.server.Status.OK;
import static nl.ulso.dictionary.Dictionary.emptyDictionary;
import static org.assertj.core.api.Assertions.assertThat;

class CuratorListRequestProcessorTest
{
    @Test
    void pathTemplate()
    {
        var processor = new CuratorListRequestProcessor(emptyMap());
        assertThat(processor.pathTemplate()).isEqualTo("/curators");
    }

    @Test
    void processReturnsSortedCuratorNamesInJson()
    {
        var dummyCurator = new CuratorStub();
        var processor = new CuratorListRequestProcessor(Map.of(
            "Music", dummyCurator,
            "books", dummyCurator,
            "Articles", dummyCurator
        ));
        var result = processor.process(emptyDictionary(), emptyDictionary(), JSON);
        assertThat(result.status()).isEqualTo(OK);
        assertThat(result.outputFormat()).isEqualTo(JSON);
        assertThat(result.output()).contains("{\"curators\":[\"articles\",\"books\",\"music\"]}");
    }

    @Test
    void processWithEmptyCuratorsMap()
    {
        var processor = new CuratorListRequestProcessor(Map.of());
        var result = processor.process(emptyDictionary(), emptyDictionary(), JSON);
        assertThat(result.status()).isEqualTo(OK);
        assertThat(result.outputFormat()).isEqualTo(JSON);
        assertThat(result.output()).contains("{\"curators\":[]}");
    }
}
