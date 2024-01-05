package nl.ulso.markdown_curator.query.builtin;

import nl.ulso.markdown_curator.query.QueryDefinitionStub;
import nl.ulso.markdown_curator.query.QueryResultFactory;
import nl.ulso.markdown_curator.vault.VaultStub;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class TableQueryTest
{
    @Test
    void configurationOptions()
    {
        var query = new TableQuery(null, new QueryResultFactory());
        assertThat(query.supportedConfiguration())
                .containsOnlyKeys("folder", "recurse", "reverse", "columns", "sort");
    }

    @ParameterizedTest
    @MethodSource("provideConfigurations")
    void configurations(Map<String, ?> configuration, String expectedOutput)
    {
        var vault = testVault();
        var document = vault.resolveDocumentInPath("C/V");
        var query = new TableQuery(vault, new QueryResultFactory());
        QueryDefinitionStub definition = new QueryDefinitionStub(query, document);
        configuration.forEach(definition::withConfiguration);
        var result = query.run(definition);
        assertThat(result.toMarkdown()).isEqualTo(expectedOutput);
    }

    static Stream<Arguments> provideConfigurations()
    {
        return Stream.of(
                Arguments.of(
                        emptyMap(), """
                                
                                | Name |
                                | ---- |
                                | [[M]] |
                                | [[V]] |
                                """
                ),
                Arguments.of(
                        Map.of("columns", List.of("date")), """
                                
                                | Name  | Date |
                                | ----- | ---- |
                                | [[M]] | 1977‑11‑11 |
                                | [[V]] | 1976‑11‑30 |
                                """
                ),
                Arguments.of(
                        Map.of("columns", List.of("date"),
                                "sort", "date",
                                "recurse", true,
                                "reverse", true), """
                                
                                | Date       | Name |
                                | ---------- | ---- |
                                | 2003‑08‑05 | [[Y]] |
                                | 1977‑11‑11 | [[M]] |
                                | 1976‑11‑30 | [[V]] |
                                """
                ),
                Arguments.of(
                        Map.of("folder", "X"), "No results"
                )
        );
    }

    private VaultStub testVault()
    {
        var vault = new VaultStub();
        vault.addDocumentInPath("C/V", """
                ---
                date: 1976-11-30
                ---
                """);
        vault.addDocumentInPath("C/M", """
                ---
                date: 1977-11-11
                ---
                """);
        vault.addDocumentInPath("C/C/Y", """
                ---
                date: 2003-08-05
                ---
                """);
        vault.addFolder("X");
        return vault;
    }
}
