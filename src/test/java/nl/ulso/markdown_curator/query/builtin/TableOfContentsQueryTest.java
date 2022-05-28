package nl.ulso.markdown_curator.query.builtin;

import nl.ulso.markdown_curator.query.QueryDefinitionStub;
import nl.ulso.markdown_curator.vault.FolderStub;
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
class TableOfContentsQueryTest
{
    @Test
    void configurationOptions()
    {
        var query = new TableOfContentsQuery();
        assertThat(query.supportedConfiguration()).containsOnlyKeys("minimum-level",
                "maximum-level");
    }

    @ParameterizedTest
    @MethodSource("provideConfigurations")
    void configurations(Map<String, ?> configuration, String expectedOutput)
    {
        var folder = new FolderStub(null, "root");
        var document = folder.addDocument("toc", """
                # One
                            
                ## Two
                            
                ## Three
                            
                ### Four
                            
                ### Five
                            
                ## Six
                """);
        var query = new TableOfContentsQuery();
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
                                - [[#Two]]
                                - [[#Three]]
                                  - [[#Four]]
                                  - [[#Five]]
                                - [[#Six]]
                                """
                ),
                Arguments.of(
                        Map.of("minimum-level", 1), """
                                - [[#One]]
                                  - [[#Two]]
                                  - [[#Three]]
                                    - [[#Four]]
                                    - [[#Five]]
                                  - [[#Six]]
                                """
                ),
                Arguments.of(
                        Map.of("maximum-level", 2), """
                                - [[#Two]]
                                - [[#Three]]
                                - [[#Six]]
                                """
                )
        );
    }
}
