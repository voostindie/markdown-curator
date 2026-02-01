package nl.ulso.curator.addon.journal;

import nl.ulso.curator.query.*;
import nl.ulso.curator.vault.VaultStub;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static nl.ulso.curator.query.QueryModuleTest.createQueryResultFactory;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class MarkedQueryTest
{
    private Journal journal;
    private MarkedQuery query;

    @BeforeEach
    void setUp()
    {
        journal = JournalTest.createTestJournal();
        query = createQuery();
    }

    @Test
    void name()
    {
        assertThat(query.name()).isEqualTo("marked");
    }

    @Test
    void description()
    {
        assertThat(query.description()).contains(
            "Generates an overview of the marked lines for the selected document");
    }

    @Test
    void supportedConfiguration()
    {
        assertThat(query.supportedConfiguration()).containsOnlyKeys("document", "markers", "level");
    }

    @Test
    void basicMarkers()
    {
        var document =
            ((VaultStub) journal.vault()).resolveDocumentInPath("Projects/foo");
        var definition = new QueryDefinitionStub(query, document)
            .withConfiguration("markers", List.of("❗️", "❓"));
        var result = query.run(definition);
        assertThat(result.toMarkdown()).isEqualTo("""
            ## ❗️
            
            - Remember this
            - Remember this too
            
            ## ❓
            
            - Important question!
            
            """);
    }

    @Test
    void nestedMarkers()
    {
        var document = ((VaultStub) journal.vault()).resolveDocumentInPath("Projects/bar");
        var definition = new QueryDefinitionStub(query, document)
            .withConfiguration("markers", "❗️");
        var result = query.run(definition);
        assertThat(result.toMarkdown()).isEqualTo("""
            ## ❗️
            
            - baR marker
            
            """);
    }

    @Test
    void customTitleMarker()
    {
        var document = ((VaultStub) journal.vault()).resolveDocumentInPath("Projects/bar");
        var definition = new QueryDefinitionStub(query, document)
            .withConfiguration("markers", "❌");
        var result = query.run(definition);
        assertThat(result.toMarkdown()).isEqualTo("""
            ## CUSTOM TITLE
            
            - Special marker
            
            """);
    }

    @Test
    void groupByDateMarker()
    {
        var document = ((VaultStub) journal.vault()).resolveDocumentInPath("Projects/Project 42");
        var definition = (QueryDefinition) document.fragments().get(1);
        var result = query.run(definition);
        assertThat(result.toMarkdown()).isEqualTo("""
            ## Status log
            
            - [[2024-08-11]]:
                - Entry one
                - Entry two
            - [[2024-08-12]]:
                - Entry three
            
            """);

    }

    private MarkedQuery createQuery()
    {
        return new MarkedQuery(journal, createQueryResultFactory());
    }
}
