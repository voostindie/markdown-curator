package nl.ulso.markdown_curator.query;

import nl.ulso.markdown_curator.vault.VaultStub;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class ListQueryTest
{
    @Test
    void configurationOptions()
    {
        var query = new ListQuery(null);
        assertThat(query.supportedConfiguration()).containsOnlyKeys("folder", "recurse", "reverse");
    }

    @ParameterizedTest
    @MethodSource("provideConfigurations")
    void configurations(Map<String, ?> configuration, String expectedOutput)
    {
        var vault = testVault();
        var document = vault.resolveDocumentInPath("A/1");
        var query = new ListQuery(vault);
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
                                - [[1]]
                                - [[2]]
                                """
                ),
                Arguments.of(
                        Map.of("reverse", true), """
                                - [[2]]
                                - [[1]]
                                """
                ),
                Arguments.of(
                        Map.of("recurse", true), """
                                - [[1]]
                                - [[2]]
                                - [[3]]
                                """
                ),
                Arguments.of(
                        Map.of("recurse", true, "reverse", true), """
                                - [[3]]
                                - [[2]]
                                - [[1]]
                                """
                ),
                Arguments.of(
                        Map.of("folder", "B"), "No results"
                )
        );
    }

    private VaultStub testVault()
    {
        var vault = new VaultStub();
        vault.addDocumentInPath("A/1", "First");
        vault.addDocumentInPath("A/2", "Second");
        vault.addDocumentInPath("A/AA/3", "Second");
        vault.addFolder("B");
        return vault;
    }
}
