package nl.ulso.markdown_curator.query.builtin;

import nl.ulso.markdown_curator.query.QueryDefinitionStub;
import nl.ulso.markdown_curator.vault.VaultStub;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
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
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void configurationOptions()
    {
        var query = new BacklinksQuery(null);
        assertThat(query.supportedConfiguration())
                .containsOnlyKeys("document");
    }

    @ParameterizedTest
    @MethodSource("provideConfigurations")
    void configurations(String documentName, String expectedOutput)
    {
        var vault = testVault();
        var document = vault.resolveDocumentInPath(documentName);
        BacklinksModel model = new BacklinksModel(vault);
        model.vaultChanged(vaultRefreshed());
        var query = new BacklinksQuery(model);
        QueryDefinitionStub definition = new QueryDefinitionStub(query, document);
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

    private VaultStub testVault()
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
        return vault;
    }
}
