package nl.ulso.curator.server;

import nl.ulso.curator.vault.VaultStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static nl.ulso.curator.query.OutputFormat.JSON;
import static nl.ulso.curator.query.OutputFormat.MARKDOWN;
import static nl.ulso.curator.server.Status.CLIENT_ERROR;
import static nl.ulso.curator.server.Status.NOT_FOUND;
import static nl.ulso.curator.server.Status.OK;
import static nl.ulso.dictionary.Dictionary.emptyDictionary;
import static nl.ulso.dictionary.Dictionary.mapDictionary;
import static org.assertj.core.api.Assertions.assertThat;

class DocumentRequestProcessorTest
{
    private VaultStub vault;
    private DocumentRequestProcessor processor;

    @BeforeEach
    void setUp()
    {
        vault = new VaultStub();
        var curator = new CuratorStub(vault);
        processor = new DocumentRequestProcessor(Map.of("testCurator", curator));
    }

    @Test
    void pathTemplate()
    {
        assertThat(processor.pathTemplate()).isEqualTo("/curators/{curator}/document");
    }

    @Test
    void missingDocumentNameQueryParamReturnsClientError()
    {
        var pathParams = mapDictionary(Map.of("curator", "testCurator"));
        var result = processor.process(pathParams, emptyDictionary(), MARKDOWN);
        assertThat(result.status()).isEqualTo(CLIENT_ERROR);
        assertThat(result.output()).contains("Document name is missing in the query parameters.");
    }

    @Test
    void documentNotFoundReturnsNotFound()
    {
        var pathParams = mapDictionary(Map.of("curator", "testCurator"));
        var queryParams = mapDictionary(Map.of("name", "NonExistentDoc"));
        var result = processor.process(pathParams, queryParams, MARKDOWN);
        assertThat(result.status()).isEqualTo(NOT_FOUND);
        assertThat(result.output()).contains("Document not found: NonExistentDoc");
    }

    @Test
    void markdownOutputGenerationForComplexDocument()
    {
        var markdown = """
            ---
            author: Alice
            tags: [java, curator]
            ---
            # Main Title

            Introduction paragraph.

            ```java
            System.out.println(42);
            ```

            <!--query:configuration-->
            query content
            <!--/query (hash123)-->

            ## Subsection

            Subsection text content.
            """;

        vault.addDocument("Doc1", markdown);

        var pathParams = mapDictionary(Map.of("curator", "testCurator"));
        var queryParams = mapDictionary(Map.of("name", "Doc1"));

        var result = processor.process(pathParams, queryParams, MARKDOWN);

        assertThat(result.status()).isEqualTo(OK);
        assertThat(result.outputFormat()).isEqualTo(MARKDOWN);
        assertThat(result.output()).hasValueSatisfying(output ->
            assertThat(output)
                .contains(
                    "author: Alice",
                    "# Main Title",
                    "System.out.println(42);",
                    "<!--query:configuration-->",
                    "## Subsection",
                    "Subsection text content."
                )
                .doesNotContain("query content")
        );
    }

    @Test
    void jsonOutputGenerationForComplexDocument()
    {
        var markdown = """
            ---
            author: Alice
            tags: [java, curator]
            ---
            # Main Title

            Introduction text.

            ```java
            int x = 1;
            ```

            <!--query:custom foo: bar-->
            output line
            <!--/query (hash999)-->

            ## Child Section

            More text.
            """;

        vault.addDocument("Doc1", markdown);

        var pathParams = mapDictionary(Map.of("curator", "testCurator"));
        var queryParams = mapDictionary(Map.of("name", "Doc1"));

        var result = processor.process(pathParams, queryParams, JSON);

        assertThat(result.status()).isEqualTo(OK);
        assertThat(result.outputFormat()).isEqualTo(JSON);
        assertThat(result.output()).hasValueSatisfying(jsonOutput ->
            assertThat(jsonOutput).contains(
                "\"name\":\"Doc1\"",
                "\"title\":\"Main Title\"",
                "\"last-modified\":0",
                "\"type\":\"frontmatter\"",
                "\"author\":\"Alice\"",
                "\"tags\":[\"java\",\"curator\"]",
                "\"type\":\"section\"",
                "\"title\":\"Main Title\"",
                "\"level\":1",
                "\"type\":\"text\"",
                "Introduction text.",
                "\"type\":\"code\"",
                "\"language\":\"java\"",
                "\"contents\":\"int x = 1;\"",
                "\"type\":\"query\"",
                "\"name\":\"custom\"",
                "\"foo\":\"bar\"",
                "\"title\":\"Child Section\"",
                "\"level\":2"
            )
        );
    }
}
