package nl.ulso.curator.query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static java.util.Locale.ENGLISH;
import static nl.ulso.curator.query.OutputFormat.MARKDOWN;
import static org.assertj.core.api.Assertions.assertThat;

class DefaultQueryResultFormatterRegistryTest
{
    private DynamicQueryResultFormatterRegistry registry;

    @BeforeEach
    void setUp()
    {
        Set<QueryResultFormatter<?>> formatters = Set.of(
            new StringMarkdownFormatter(),
            new UnorderedListMarkdownFormatter(),
            new TableMarkdownFormatter(),
            new ErrorMarkdownFormatter(),
            new EmptyMarkdownFormatter(new ResourceBundleGeneralMessages(ENGLISH))
        );
        registry = new DefaultQueryResultFormatterRegistry(formatters);
    }

    @Test
    void unknownOutputFormatFallsBackToMarkdown()
    {
        var formatted = registry.formatResult(
            new StringResult("unknown"),
            new OutputFormat("UNKNOWN", "application/unknown")
        );
        assertThat(formatted.outputFormat()).isEqualTo(MARKDOWN);
        assertThat(formatted.output()).isEqualTo("unknown");
    }

    @Test
    void unsupportedResultTypeInNewFormatFallsBackToMarkdown()
    {
        var format = new OutputFormat("dummy", "application/x-dummy");
        registry.replaceDynamicFormatters(Set.of(new DummyFormatter(format, "dummy")));
        var formatted = registry.formatResult(new StringResult("Hello"), format);
        assertThat(registry.supports(new StringResult("Hello"), format)).isFalse();
        assertThat(formatted.outputFormat()).isEqualTo(MARKDOWN);
        assertThat(formatted.output()).isEqualTo("Hello");
    }

    @Test
    void supportedResultTypeInNewFormatIsFormattedInThatFormat()
    {
        var format = new OutputFormat("dummy", "application/x-dummy");
        registry.replaceDynamicFormatters(Set.of(new DummyFormatter(format, "dummy")));
        var formatted = registry.formatResult(new DummyQueryResult(), format);
        assertThat(formatted.outputFormat()).isEqualTo(format);
        assertThat(formatted.output()).isEqualTo("dummy");
    }

    @Test
    void unsupportedResultTypeWithoutMarkdownFallbackIsAnError()
    {
        var format = new OutputFormat("other", "application/x-other");
        var formatted = registry.formatResult(new DummyQueryResult(), format);
        assertThat(formatted.outputFormat()).isEqualTo(MARKDOWN);
        assertThat(formatted.output())
            .contains("Error")
            .contains("No output formatter for 'other'");
    }

    @Test
    void outputFormatsAreLookedUpByNameCaseInsensitively()
    {
        var format = new OutputFormat("dummy", "application/x-dummy");
        registry.replaceDynamicFormatters(Set.of(new DummyFormatter(format, "dummy")));
        assertThat(registry.outputFormat("DUMMY")).contains(format);
        assertThat(registry.outputFormat(" Markdown ")).contains(MARKDOWN);
        assertThat(registry.outputFormat("unknown")).isEmpty();
        assertThat(registry.outputFormat(null)).isEmpty();
    }

    @Test
    void requestedFormatIsResolvedByName()
    {
        var formatted = registry.formatResult(
            new StringResult("Hello"), new OutputFormat("markdown", "text/plain"));
        assertThat(formatted.outputFormat()).isSameAs(MARKDOWN);
    }

    @Test
    void conflictingDynamicFormatIsIgnored()
    {
        var conflicting = new OutputFormat("markdown", "text/x-markdown");
        registry.replaceDynamicFormatters(Set.of(new DummyFormatter(conflicting, "conflict")));
        assertThat(registry.outputFormats()).containsExactly(MARKDOWN);
        assertThat(registry.supports(new DummyQueryResult(), MARKDOWN)).isFalse();
    }

    @Test
    void staticOutputFormatsDoNotChange()
    {
        var format = new OutputFormat("dummy", "application/x-dummy");
        registry.replaceDynamicFormatters(Set.of(new DummyFormatter(format, "dummy")));
        assertThat(registry.staticOutputFormats()).containsExactly(MARKDOWN);
        assertThat(registry.outputFormats()).containsExactly(MARKDOWN, format);
    }

    @Test
    void unknownQueryResult()
    {
        var output = registry.format(
            new DummyQueryResult(),
            MARKDOWN
        );
        assertThat(output)
            .contains("Error")
            .contains(
                "No output formatter for 'markdown' available for query result type " +
                "'DummyQueryResult'");
    }

    @Test
    void stringResult()
    {
        var output = registry.format(
            new StringResult("Hello, world"),
            MARKDOWN
        );
        assertThat(output).contains("Hello, world");
    }

    /// No specific formatter exists for the [EmptyResult], but the type extends [StringResult],
    /// so the [StringMarkdownFormatter] should kick in.
    @Test
    void emptyResult()
    {
        var output = registry.format(
            new EmptyResult(),
            MARKDOWN
        );
        assertThat(output).contains("No results");
    }

    @Test
    void stringResultFromUnknownFormatWithMarkdownMimeType()
    {
        var output = registry.format(
            new StringResult("Hello, world"),
            new OutputFormat("NEW", "text/markdown")
        );
        assertThat(output).contains("Hello, world");
    }

    @Test
    void dynamicFormatterForNewResultType()
    {
        registry.replaceDynamicFormatters(Set.of(new DummyMarkdownFormatter("dynamic")));
        assertThat(registry.supports(new DummyQueryResult(), MARKDOWN)).isTrue();
        assertThat(registry.format(new DummyQueryResult(), MARKDOWN)).isEqualTo("dynamic");
    }

    @Test
    void replacingDynamicFormattersRemovesPreviousOnes()
    {
        registry.replaceDynamicFormatters(Set.of(new DummyMarkdownFormatter("dynamic")));
        registry.replaceDynamicFormatters(Set.of());
        assertThat(registry.supports(new DummyQueryResult(), MARKDOWN)).isFalse();
        assertThat(registry.format(new StringResult("static"), MARKDOWN)).contains("static");
    }

    @Test
    void dynamicFormatterAddsOutputFormat()
    {
        var format = new OutputFormat("DUMMY", "application/x-dummy");
        registry.replaceDynamicFormatters(Set.of(new DummyFormatter(format, "dummy")));
        assertThat(registry.outputFormats()).contains(MARKDOWN, format);
        assertThat(registry.format(new DummyQueryResult(), format)).isEqualTo("dummy");
        registry.replaceDynamicFormatters(Set.of());
        assertThat(registry.outputFormats()).containsExactly(MARKDOWN);
    }

    @Test
    void staticFormatterWinsOverDynamicFormatterForSameResultType()
    {
        registry.replaceDynamicFormatters(Set.of(new StringOverrideFormatter()));
        assertThat(registry.format(new StringResult("Hello"), MARKDOWN))
            .isNotEqualTo("override");
    }

    @Test
    void mostSpecificDynamicFormatterWinsOverStaticFormatter()
    {
        var registry = new DefaultQueryResultFormatterRegistry(Set.of(new AnimalFormatter()));
        registry.replaceDynamicFormatters(Set.of(new DogFormatter()));
        assertThat(registry.format(new Dog(), MARKDOWN)).isEqualTo("dog");
        assertThat(registry.format(new Cat(), MARKDOWN)).isEqualTo("animal");
    }

    @Test
    void mostSpecificStaticFormatterWins()
    {
        var registry = new DefaultQueryResultFormatterRegistry(
            Set.of(new AnimalFormatter(), new DogFormatter()));
        assertThat(registry.format(new Dog(), MARKDOWN)).isEqualTo("dog");
        assertThat(registry.format(new Cat(), MARKDOWN)).isEqualTo("animal");
    }

    private static final class DummyQueryResult
        implements QueryResult
    {
    }

    private record DummyFormatter(OutputFormat outputFormat, String output)
        implements QueryResultFormatter<DummyQueryResult>
    {
        @Override
        public Class<DummyQueryResult> queryResultType()
        {
            return DummyQueryResult.class;
        }

        @Override
        public String format(DummyQueryResult result)
        {
            return output;
        }
    }

    private static final class DummyMarkdownFormatter
        implements QueryResultFormatter<DummyQueryResult>
    {
        private final String output;

        DummyMarkdownFormatter(String output)
        {
            this.output = output;
        }

        @Override
        public Class<DummyQueryResult> queryResultType()
        {
            return DummyQueryResult.class;
        }

        @Override
        public OutputFormat outputFormat()
        {
            return MARKDOWN;
        }

        @Override
        public String format(DummyQueryResult result)
        {
            return output;
        }
    }

    private static final class StringOverrideFormatter
        implements QueryResultFormatter<StringResult>
    {
        @Override
        public Class<StringResult> queryResultType()
        {
            return StringResult.class;
        }

        @Override
        public OutputFormat outputFormat()
        {
            return MARKDOWN;
        }

        @Override
        public String format(StringResult result)
        {
            return "override";
        }
    }

    private interface Animal
        extends QueryResult
    {
    }

    private record Dog()
        implements Animal
    {
    }

    private record Cat()
        implements Animal
    {
    }

    private static final class AnimalFormatter
        implements QueryResultFormatter<Animal>
    {
        @Override
        public Class<Animal> queryResultType()
        {
            return Animal.class;
        }

        @Override
        public OutputFormat outputFormat()
        {
            return MARKDOWN;
        }

        @Override
        public String format(Animal result)
        {
            return "animal";
        }
    }

    private static final class DogFormatter
        implements QueryResultFormatter<Dog>
    {
        @Override
        public Class<Dog> queryResultType()
        {
            return Dog.class;
        }

        @Override
        public OutputFormat outputFormat()
        {
            return MARKDOWN;
        }

        @Override
        public String format(Dog result)
        {
            return "dog";
        }
    }
}
