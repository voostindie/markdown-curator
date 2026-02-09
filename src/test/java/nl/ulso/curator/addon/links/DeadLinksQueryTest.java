package nl.ulso.curator.addon.links;

import nl.ulso.curator.query.QueryDefinitionStub;
import nl.ulso.curator.vault.VaultStub;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static nl.ulso.curator.change.ChangeCollector.newChangeCollector;
import static nl.ulso.curator.query.QueryModuleTest.createQueryResultFactory;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class DeadLinksQueryTest
{
    @Test
    void configurationOptions()
    {
        var query = new DeadLinksQuery(null, createQueryResultFactory());
        assertThat(query.supportedConfiguration())
                .containsOnlyKeys("document");
    }

    @ParameterizedTest
    @MethodSource("provideConfigurations")
    void queryDeadLinks(String documentName, String expectedOutput)
    {
        var vault = new VaultStub();
        vault.addDocumentInPath("foo", "[[zzz]], [[bar]] and [[baz]]");
        vault.addDocumentInPath("bar", "");
        var model = new LinksModel(vault);
        model.reset(newChangeCollector());
        var query = new DeadLinksQuery(model, createQueryResultFactory());
        var definition = new QueryDefinitionStub(query, vault.resolveDocumentInPath(documentName));
        var result = query.run(definition);
        assertThat(result.toMarkdown()).isEqualTo(expectedOutput);
    }

    public static Stream<Arguments> provideConfigurations()
    {
        return Stream.of(
                Arguments.of("foo", """
                        - [[baz]]
                        - [[zzz]]
                        """),
                Arguments.of("bar", "No results")
        );
    }
}
