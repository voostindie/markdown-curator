package nl.ulso.markdown_curator.journal;

import nl.ulso.markdown_curator.query.QueryDefinitionStub;
import nl.ulso.markdown_curator.query.QueryResultFactory;
import nl.ulso.markdown_curator.vault.VaultStub;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

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
        var query = createQuery();
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
                ## ❓
                
                - Important question!
                
                ## ❗️
                
                - Remember this
                - Remember this too
                
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

    private MarkedQuery createQuery()
    {
        return new MarkedQuery(journal, new QueryResultFactory());
    }
}
