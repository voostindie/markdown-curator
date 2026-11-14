package nl.ulso.curator.main;

import nl.ulso.curator.extension.*;
import nl.ulso.dictionary.Dictionary;

import java.util.*;
import java.util.function.BiFunction;

/// Configurable [ExtensionFactory] for tests.
final class ExtensionFactoryStub
    implements ExtensionFactory
{
    private final BiFunction<Dictionary, ExtensionContext, Extension> creator;
    private final Set<Class<?>> requires = new HashSet<>();
    private final Set<Class<?>> provides = new HashSet<>();
    private final List<Contribution> contributions = new ArrayList<>();
    private final Map<String, String> settings = new HashMap<>();

    private ExtensionFactoryStub(BiFunction<Dictionary, ExtensionContext, Extension> creator)
    {
        this.creator = creator;
    }

    static ExtensionFactoryStub factory(BiFunction<Dictionary, ExtensionContext, Extension> creator)
    {
        return new ExtensionFactoryStub(creator);
    }

    ExtensionFactoryStub requiring(Class<?>... types)
    {
        requires.addAll(List.of(types));
        return this;
    }

    ExtensionFactoryStub providing(Class<?>... types)
    {
        provides.addAll(List.of(types));
        return this;
    }

    <T> ExtensionFactoryStub contributing(ExtensionPoint<T> extensionPoint, T value)
    {
        contributions.add(extensionPoint.contribute(value));
        return this;
    }

    ExtensionFactoryStub withSettings(String... names)
    {
        Arrays.stream(names).forEach(name -> settings.put(name, "Setting " + name));
        return this;
    }

    @Override
    public String description()
    {
        return "Stub";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return settings;
    }

    @Override
    public Set<Class<?>> requires()
    {
        return requires;
    }

    @Override
    public Set<Class<?>> provides()
    {
        return provides;
    }

    @Override
    public List<Contribution> contributions(Dictionary configuration)
    {
        return contributions;
    }

    @Override
    public Extension create(Dictionary configuration, ExtensionContext context)
    {
        return creator.apply(configuration, context);
    }
}
