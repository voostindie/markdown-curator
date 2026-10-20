package nl.ulso.curator.query;

import nl.ulso.curator.vault.VaultStub;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static nl.ulso.curator.query.OutputFormat.MARKDOWN;
import static nl.ulso.curator.query.QueryTestModule.createQueryCatalog;
import static nl.ulso.curator.query.QueryTestModule.createQueryResultFactory;
import static nl.ulso.curator.query.QueryTestModule.createQueryResultFormatterRegistry;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class ExportQueryTest
{
    private final QueryResultFactory resultFactory = createQueryResultFactory();
    private final QueryResultFormatterRegistry formatterRegistry =
        createQueryResultFormatterRegistry();

    @Test
    void name()
    {
        var query = new ExportQuery(new VaultStub(), QueryTestModule.createEmptyCatalog(),
            formatterRegistry, resultFactory
        );
        assertThat(query.name()).isEqualTo("export");
    }

    @Test
    void description()
    {
        var query = new ExportQuery(new VaultStub(), QueryTestModule.createEmptyCatalog(),
            formatterRegistry, resultFactory
        );
        assertThat(query.description()).isNotBlank();
    }

    @Test
    void supportedConfiguration()
    {
        var query = new ExportQuery(new VaultStub(), QueryTestModule.createEmptyCatalog(),
            formatterRegistry, resultFactory
        );
        assertThat(query.supportedConfiguration())
            .containsOnlyKeys("document", "strip-emojis-from-links");
    }

    @Test
    void isImpactedByAlwaysReturnsFalse()
    {
        var query = new ExportQuery(new VaultStub(), QueryTestModule.createEmptyCatalog(),
            formatterRegistry, resultFactory
        );
        assertThat(query.isImpactedBy(null, null)).isFalse();
    }

    @Test
    void runWithoutDocumentConfigurationReturnsError()
    {
        var vault = new VaultStub();
        var query = new ExportQuery(vault, QueryTestModule.createEmptyCatalog(),
            formatterRegistry, resultFactory
        );
        var definition = new QueryDefinitionStub(query, null);
        var result = query.run(definition);
        assertThat(result).isInstanceOf(ErrorResult.class);
        var error = (ErrorResult) result;
        assertThat(error.errorMessage()).contains("No document specified");
    }

    @Test
    void runWithNonExistentDocumentReturnsError()
    {
        var vault = new VaultStub();
        var query = new ExportQuery(vault, QueryTestModule.createEmptyCatalog(),
            formatterRegistry, resultFactory
        );
        var definition = new QueryDefinitionStub(query, null)
            .withConfiguration("document", "NonExistentDoc");
        var result = query.run(definition);
        assertThat(result).isInstanceOf(ErrorResult.class);
        var error = (ErrorResult) result;
        assertThat(error.errorMessage()).contains("Document not found: NonExistentDoc");
    }

    @Test
    void exportStripsFrontMatter()
    {
        var vault = new VaultStub();
        vault.addDocument("doc1", """
            ---
            title: Test Document
            date: 2026-10-20
            tags: [a, b]
            ---
            # Heading 1
            
            Body text without front matter.
            """
        );
        var query = new ExportQuery(vault, QueryTestModule.createEmptyCatalog(),
            formatterRegistry, resultFactory
        );
        var definition = new QueryDefinitionStub(query, null)
            .withConfiguration("document", "doc1");
        var result = query.run(definition);
        assertThat(result).isInstanceOf(StringResult.class);
        var markdown = formatterRegistry.format(result, MARKDOWN);
        assertThat(markdown).isEqualTo("""
            # Heading 1
            
            Body text without front matter.
            """);
    }

    @ParameterizedTest
    @MethodSource("wikilinkReplacementExamples")
    void exportReplacesWikilinksWithPlainText(String inputContent, String expectedOutput)
    {
        var vault = new VaultStub();
        vault.addDocument("testDoc", inputContent);
        var query = new ExportQuery(vault, QueryTestModule.createEmptyCatalog(),
            formatterRegistry, resultFactory
        );
        var definition = new QueryDefinitionStub(query, null)
            .withConfiguration("document", "testDoc");
        var result = query.run(definition);
        assertThat(result).isInstanceOf(StringResult.class);
        var markdown = formatterRegistry.format(result, MARKDOWN);
        assertThat(markdown).isEqualTo(expectedOutput);
    }

    static Stream<Arguments> wikilinkReplacementExamples()
    {
        return Stream.of(
            Arguments.of(
                "This links to [[Internal link]].\n",
                "This links to Internal link.\n"
            ),
            Arguments.of(
                "This links to [[Internal link|Alias]].\n",
                "This links to Alias.\n"
            ),
            Arguments.of(
                "This links to [[Internal link#Heading]].\n",
                "This links to Internal link > Heading.\n"
            ),
            Arguments.of(
                "This links to [[Internal link#Heading|Alias]].\n",
                "This links to Alias.\n"
            ),
            Arguments.of(
                "Multiple links: [[Doc A]], [[Doc B|B-Alias]], and [[Doc C#Section C]].\n",
                "Multiple links: Doc A, B-Alias, and Doc C > Section C.\n"
            ),
            Arguments.of(
                "## Section with [[Link In Title|Title Alias]]\n\nContent here.\n",
                "## Section with Title Alias\n\nContent here.\n"
            ),
            Arguments.of(
                "Unclosed [[ link is left as is.\n",
                "Unclosed [[ link is left as is.\n"
            ),
            Arguments.of(
                "Link to same page anchor: [[#Heading]] and [[#Heading|Anchor Alias]].\n",
                "Link to same page anchor: Heading and Anchor Alias.\n"
            )
        );
    }

    @Test
    void exportPreservesCodeBlocks()
    {
        var vault = new VaultStub();
        vault.addDocument("docWithCode", """
            # Code Sample
            
            Here is some code:
            
            ```java
            public class Foo {
                // [[Do not replace wikilinks in code]]
            }
            ```
            
            End of document.
            """
        );
        var query = new ExportQuery(vault, QueryTestModule.createEmptyCatalog(),
            formatterRegistry, resultFactory
        );
        var definition = new QueryDefinitionStub(query, null)
            .withConfiguration("document", "docWithCode");
        var result = query.run(definition);
        var markdown = formatterRegistry.format(result, MARKDOWN);
        assertThat(markdown).isEqualTo("""
            # Code Sample
            
            Here is some code:
            
            ```java
            public class Foo {
                // [[Do not replace wikilinks in code]]
            }
            ```
            
            End of document.
            """);
    }

    @Test
    void exportExecutesQueriesOnTheFlyAndStripsHtmlCommentConfigurations()
    {
        var vault = new VaultStub();
        var folder = vault.addFolder("notes");
        folder.addDocument("Page 1", "# Page 1");
        folder.addDocument("Page 2", "# Page 2");

        vault.addDocument("overview", """
            # Overview
            
            <!--query:list
            folder: notes
            -->
            Old stale list output
            <!--/query (12345)-->
            
            End of overview.
            """
        );

        var listQuery = new ListQuery(vault, resultFactory);
        var catalog = createQueryCatalog(Set.of(listQuery));
        var exportQuery = new ExportQuery(vault, catalog, formatterRegistry, resultFactory);
        var definition = new QueryDefinitionStub(exportQuery, null)
            .withConfiguration("document", "overview");

        var result = exportQuery.run(definition);
        var markdown = formatterRegistry.format(result, MARKDOWN);

        assertThat(markdown).isEqualTo("""
            # Overview
            
            - Page 1
            - Page 2
            
            End of overview.
            """);
    }

    @Test
    void exportHandlesFailingQueryGracefully()
    {
        var vault = new VaultStub();
        vault.addDocument("docWithFailingQuery", """
            # Document
            <!--query:failing
            -->
            <!--/query-->
            After query.
            """
        );

        Query failingQuery = new Query()
        {
            @Override
            public String name()
            {
                return "failing";
            }

            @Override
            public String description()
            {
                return "Fails";
            }

            @Override
            public Map<String, String> supportedConfiguration()
            {
                return emptyMap();
            }

            @Override
            public boolean isImpactedBy(
                nl.ulso.curator.change.Changelog changelog, QueryDefinition definition)
            {
                return false;
            }

            @Override
            public QueryResult run(QueryDefinition definition)
            {
                throw new RuntimeException("Simulated query failure");
            }
        };

        var catalog = createQueryCatalog(Set.of(failingQuery));
        var exportQuery = new ExportQuery(vault, catalog, formatterRegistry, resultFactory);
        var definition = new QueryDefinitionStub(exportQuery, null)
            .withConfiguration("document", "docWithFailingQuery");

        var result = exportQuery.run(definition);
        var markdown = formatterRegistry.format(result, MARKDOWN);

        assertThat(markdown).isEqualTo("""
            # Document
            After query.
            """);
    }

    @Test
    void exportWithStripEmojisFromLinksFalsePreservesEmojisInLinksByDefault()
    {
        var vault = new VaultStub();
        vault.addDocument("docWithEmojis", """
            # Heading with 🚀

            See [[🚀 Rocket Document|🔥 Rocket Alias]] and [[💡 Idea#📌 Pin]].
            """);
        var query = new ExportQuery(vault, QueryTestModule.createEmptyCatalog(),
            formatterRegistry, resultFactory
        );
        var definition = new QueryDefinitionStub(query, null)
            .withConfiguration("document", "docWithEmojis");

        var result = query.run(definition);
        var markdown = formatterRegistry.format(result, MARKDOWN);

        assertThat(markdown).isEqualTo("""
            # Heading with 🚀

            See 🔥 Rocket Alias and 💡 Idea > 📌 Pin.
            """);
    }

    @Test
    void exportWithStripEmojisFromLinksExplicitlyFalsePreservesEmojisInLinks()
    {
        var vault = new VaultStub();
        vault.addDocument("docWithEmojis", """
            # Heading with 🚀

            See [[🚀 Rocket Document]].
            """);
        var query = new ExportQuery(vault, QueryTestModule.createEmptyCatalog(),
            formatterRegistry, resultFactory
        );
        var definition = new QueryDefinitionStub(query, null)
            .withConfiguration("document", "docWithEmojis")
            .withConfiguration("strip-emojis-from-links", false);

        var result = query.run(definition);
        var markdown = formatterRegistry.format(result, MARKDOWN);

        assertThat(markdown).isEqualTo("""
            # Heading with 🚀

            See 🚀 Rocket Document.
            """);
    }

    @Test
    void exportWithStripEmojisFromLinksTrueStripsEmojisFromLinksOnly()
    {
        var vault = new VaultStub();
        vault.addDocument("docWithEmojis", """
            # Heading with 🚀 and [[⭐ Star Document]]

            Here is regular text with 🌟 emojis.
            Link 1: [[🚀 Rocket Document]]
            Link 2: [[🚀 Rocket Document|🔥 Rocket Alias]]
            Link 3: [[💡 Idea#📌 Pin]]
            Link 4: [[💡 Idea#📌 Pin|🔥 Rocket Alias]]
            """);
        var query = new ExportQuery(vault, QueryTestModule.createEmptyCatalog(),
            formatterRegistry, resultFactory
        );
        var definition = new QueryDefinitionStub(query, null)
            .withConfiguration("document", "docWithEmojis")
            .withConfiguration("strip-emojis-from-links", true);

        var result = query.run(definition);
        var markdown = formatterRegistry.format(result, MARKDOWN);

        assertThat(markdown).isEqualTo("""
            # Heading with 🚀 and  Star Document

            Here is regular text with 🌟 emojis.
            Link 1:  Rocket Document
            Link 2:  Rocket Alias
            Link 3:  Idea >  Pin
            Link 4:  Rocket Alias
            """);
    }
}
