package nl.ulso.curator.extension;

import dagger.*;
import dagger.Module;
import dagger.multibindings.*;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.ChangeProcessor;
import nl.ulso.curator.change.Changelog;
import nl.ulso.curator.query.*;
import nl.ulso.curator.vault.Document;
import nl.ulso.dictionary.Dictionary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static nl.ulso.curator.change.Changelog.emptyChangelog;
import static nl.ulso.dictionary.Dictionary.emptyDictionary;
import static nl.ulso.dictionary.Dictionary.mapDictionary;
import static org.assertj.core.api.Assertions.assertThat;

/// Verifies that the recommended way of building extensions - Dagger subcomponents - works.
class ExtensionSubcomponentTest
{
    private static final ServiceRegistry NO_SERVICES = new ServiceRegistry()
    {
        @Override
        public <T> T get(Class<T> type)
        {
            throw new IllegalStateException("No services");
        }

        @Override
        public <T> java.util.List<T> all(Class<T> type)
        {
            return java.util.List.of();
        }
    };

    private TestComponent component;

    @BeforeEach
    void setUp()
    {
        component = DaggerExtensionSubcomponentTest_TestComponent.create();
    }

    @Test
    void extensionFactoryIsRegistered()
    {
        assertThat(component.extensionFactories()).containsOnlyKeys("greeting");
    }

    @Test
    void extensionExposesOnlyItsOwnBindings()
    {
        var extension = component.extensionFactories().get("greeting")
            .create(emptyDictionary(), NO_SERVICES);
        assertThat(extension.queries()).hasSize(1).first().isInstanceOf(GreetingQuery.class);
        assertThat(extension.changeProcessors()).hasSize(1)
            .first().isInstanceOf(GreetingProcessor.class);
        assertThat(extension.measurementTrackers()).isEmpty();
        assertThat(extension.services()).isEmpty();
        assertThat(extension.lifecycles()).isEmpty();
        assertThat(component.queries()).hasSize(1).first().isInstanceOf(StaticQuery.class);
    }

    @Test
    void extensionUsesConfigurationAndParentBindings()
    {
        var extension = component.extensionFactories().get("greeting")
            .create(mapDictionary(Map.of("name", "extension")), NO_SERVICES);
        var query = (GreetingQuery) extension.queries().iterator().next();
        assertThat(query.greeting()).isEqualTo("Hello, extension!");
    }

    @Test
    void extensionScopeIsPerInstance()
    {
        var factory = component.extensionFactories().get("greeting");
        var extension1 = factory.create(emptyDictionary(), NO_SERVICES);
        var extension2 = factory.create(emptyDictionary(), NO_SERVICES);
        var query1 = (GreetingQuery) extension1.queries().iterator().next();
        var processor1 = extension1.changeProcessors().iterator().next();
        var processor2 = extension2.changeProcessors().iterator().next();
        assertThat(query1.processor()).isSameAs(processor1);
        assertThat(processor1).isNotSameAs(processor2);
    }

    @Singleton
    @Component(modules = TestModule.class)
    interface TestComponent
    {
        Map<String, ExtensionFactory> extensionFactories();

        Set<Query> queries();
    }

    @Module(subcomponents = GreetingExtension.class)
    abstract static class TestModule
    {
        @Provides
        static String provideSalutation()
        {
            return "Hello";
        }

        @Binds
        @IntoSet
        abstract Query bindStaticQuery(StaticQuery query);

        @Binds
        @IntoMap
        @StringKey("greeting")
        abstract ExtensionFactory bindGreetingExtensionFactory(GreetingExtensionFactory factory);
    }

    @ExtensionScope
    @Subcomponent(modules = {ExtensionModule.class, GreetingModule.class})
    interface GreetingExtension
        extends Extension
    {
        @Subcomponent.Factory
        interface Factory
        {
            GreetingExtension create(@BindsInstance GreetingSettings settings);
        }
    }

    record GreetingSettings(String name)
    {
    }

    static final class GreetingExtensionFactory
        implements ExtensionFactory
    {
        private final GreetingExtension.Factory factory;

        @Override
        public String description()
        {
            return "";
        }

        @Inject
        GreetingExtensionFactory(GreetingExtension.Factory factory)
        {
            this.factory = factory;
        }

        @Override
        public Extension create(Dictionary configuration, ServiceRegistry serviceRegistry)
        {
            return factory.create(new GreetingSettings(configuration.string("name", "world")));
        }
    }

    @Module
    abstract static class GreetingModule
    {
        @Binds
        @IntoSet
        @ExtensionBinding
        abstract Query bindGreetingQuery(GreetingQuery query);

        @Binds
        @IntoSet
        @ExtensionBinding
        abstract ChangeProcessor bindGreetingProcessor(GreetingProcessor processor);
    }

    @ExtensionScope
    static final class GreetingProcessor
        implements ChangeProcessor
    {
        @Inject
        GreetingProcessor()
        {
        }

        @Override
        public Changelog apply(Changelog changelog)
        {
            return emptyChangelog();
        }

        @Override
        public Set<Class<?>> consumedPayloadTypes()
        {
            return Set.of(Document.class);
        }
    }

    @ExtensionScope
    static final class GreetingQuery
        extends QueryStub
    {
        private final String greeting;
        private final GreetingProcessor processor;

        @Inject
        GreetingQuery(String salutation, GreetingSettings settings, GreetingProcessor processor)
        {
            super("greeting");
            this.greeting = salutation + ", " + settings.name() + "!";
            this.processor = processor;
        }

        String greeting()
        {
            return greeting;
        }

        GreetingProcessor processor()
        {
            return processor;
        }
    }

    @Singleton
    static final class StaticQuery
        extends QueryStub
    {
        @Inject
        StaticQuery()
        {
            super("static");
        }
    }

    abstract static class QueryStub
        implements Query
    {
        private final String name;

        QueryStub(String name)
        {
            this.name = name;
        }

        @Override
        public String name()
        {
            return name;
        }

        @Override
        public String description()
        {
            return "Stub";
        }

        @Override
        public Map<String, String> supportedConfiguration()
        {
            return Map.of();
        }

        @Override
        public boolean isImpactedBy(Changelog changelog, QueryDefinition definition)
        {
            return false;
        }

        @Override
        public QueryResult run(QueryDefinition definition)
        {
            throw new UnsupportedOperationException("Not needed for this test");
        }
    }
}
