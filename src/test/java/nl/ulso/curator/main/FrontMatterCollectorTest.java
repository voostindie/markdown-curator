package nl.ulso.curator.main;

import nl.ulso.curator.vault.VaultStub;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

@ExtendWith(SoftAssertionsExtension.class)
class FrontMatterCollectorTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void addFrontMatterToDocumentWithout()
    {
        var vault = new VaultStub();
        var document = vault.addDocument("document", """
            Content
            """
        );
        var collector = new FrontMatterCollector(vault);
        collector.updateFrontMatterFor(document,
            dictionary -> dictionary.setProperty("key", "value")
        );
        var rewrites = collector.resolveFrontMatterRewrites();
        softly.assertThat(rewrites).hasSize(1);
        var dictionary = rewrites.get(document);
        softly.assertThat(dictionary.getProperty("key")).hasValue("value");
        softly.assertThat(dictionary.getProperty("generated_keys")).hasValue(List.of("key"));
        softly.assertThat(dictionary.propertyNames()).hasSize(2);
    }

    @Test
    void addFrontMatterToDocument()
    {
        var vault = new VaultStub();
        var document = vault.addDocument("document", """
            ---
            foo: bar
            ---
            Content
            """
        );
        var collector = new FrontMatterCollector(vault);
        collector.updateFrontMatterFor(document,
            dictionary -> dictionary.setProperty("key", "value")
        );
        var rewrites = collector.resolveFrontMatterRewrites();
        softly.assertThat(rewrites).hasSize(1);
        var dictionary = rewrites.get(document);
        softly.assertThat(dictionary.propertyNames()).hasSize(3);
        softly.assertThat(dictionary.getProperty("foo")).hasValue("bar");
        softly.assertThat(dictionary.getProperty("key")).hasValue("value");
        softly.assertThat(dictionary.getProperty("generated_keys")).hasValue(List.of("key"));
    }

    @Test
    void overrideFrontMatterForDocument()
    {
        var vault = new VaultStub();
        var document = vault.addDocument("document", """
            ---
            foo: bar
            ---
            Content
            """
        );
        var collector = new FrontMatterCollector(vault);
        collector.updateFrontMatterFor(document,
            dictionary -> dictionary.setProperty("foo", "baz")
        );
        var rewrites = collector.resolveFrontMatterRewrites();
        softly.assertThat(rewrites).hasSize(1);
        var dictionary = rewrites.get(document);
        softly.assertThat(dictionary.propertyNames()).hasSize(2);
        softly.assertThat(dictionary.getProperty("foo")).hasValue("baz");
        softly.assertThat(dictionary.getProperty("generated_keys")).hasValue(List.of("foo"));
    }

    @Test
    void meaninglessFrontMatterUpdateDoesNothing()
    {
        var vault = new VaultStub();
        var document = vault.addDocument("document", """
            ---
            foo: bar
            generated_keys: [foo]
            ---
            Content
            """
        );
        var collector = new FrontMatterCollector(vault);
        collector.updateFrontMatterFor(document,
            dictionary -> dictionary.setProperty("foo", "bar")
        );
        var rewrites = collector.resolveFrontMatterRewrites();
        softly.assertThat(rewrites).isEmpty();
    }

    @Test
    void removeGeneratedKeysIfFrontMatterIsNotOverridden()
    {
        var vault = new VaultStub();
        var document = vault.addDocument("document", """
            ---
            foo: bar
            generated_keys: [foo]
            ---
            Content
            """
        );
        var collector = new FrontMatterCollector(vault);
        var rewrites = collector.resolveFrontMatterRewrites();
        softly.assertThat(rewrites).hasSize(1);
        var dictionary = rewrites.get(document);
        softly.assertThat(dictionary.propertyNames()).hasSize(1);
        softly.assertThat(dictionary.getProperty("foo")).hasValue("bar");
    }
}
