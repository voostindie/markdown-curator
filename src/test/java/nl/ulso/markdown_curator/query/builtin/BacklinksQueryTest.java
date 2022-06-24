package nl.ulso.markdown_curator.query.builtin;

import nl.ulso.markdown_curator.query.QueryDefinitionStub;
import nl.ulso.markdown_curator.vault.VaultStub;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static nl.ulso.markdown_curator.vault.event.VaultChangedEvent.vaultRefreshed;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class BacklinksQueryTest
{
    @Test
    void configurationOptions()
    {
        var query = new BacklinksQuery(null);
        assertThat(query.supportedConfiguration())
                .containsOnlyKeys("document");
    }

    @ParameterizedTest
    @MethodSource("provideConfigurations")
    void queryBacklinks(String documentName, String expectedOutput)
    {
        var vault = new VaultStub();
        vault.addDocumentInPath("Home", "[[Foo]], [[Bar]] and [[Baz]]\n");
        vault.addDocumentInPath("Foo", "[[Bar]]]]");
        vault.addDocumentInPath("Bar", """
                ## Section
                                
                [[Foo]]
                """);
        vault.addDocumentInPath("Baz", """
                ## [[Foo]]
                """);
        var document = vault.resolveDocumentInPath(documentName);
        var model = new LinksModel(vault);
        model.vaultChanged(vaultRefreshed());
        var query = new BacklinksQuery(model);
        var definition = new QueryDefinitionStub(query, document);
        var result = query.run(definition);
        assertThat(result.toMarkdown()).isEqualTo(expectedOutput);
    }

    static Stream<Arguments> provideConfigurations()
    {
        return Stream.of(
                Arguments.of("Home", "No results"),
                Arguments.of("Foo", """
                        - [[Bar]]
                            - [[Bar#Section|Section]]
                        - [[Baz]]
                            - [[Baz#Foo|Foo]]
                        - [[Home]]
                        """),
                Arguments.of("Bar", """
                        - [[Foo]]
                        - [[Home]]
                        """),
                Arguments.of("Baz", """
                        - [[Home]]
                        """)
        );
    }
}
