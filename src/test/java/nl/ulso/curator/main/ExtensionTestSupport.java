package nl.ulso.curator.main;

import nl.ulso.curator.extension.ExtensionFactory;
import nl.ulso.curator.query.DynamicQueryCatalog;
import nl.ulso.curator.query.DynamicQueryResultFormatterRegistry;

import java.util.Map;

import static nl.ulso.curator.query.QueryTestModule.createQueryResultFormatterRegistry;

final class ExtensionTestSupport
{
    private ExtensionTestSupport()
    {
    }

    static ExtensionRegistry newExtensionRegistry()
    {
        return newExtensionRegistry(new DynamicQueryCatalogStub(), Map.of());
    }

    static ExtensionRegistry newExtensionRegistry(
        DynamicQueryCatalog catalog, Map<String, ExtensionFactory> factories)
    {
        return newExtensionRegistry(catalog, createQueryResultFormatterRegistry(), factories);
    }

    static ExtensionRegistry newExtensionRegistry(
        DynamicQueryCatalog catalog, DynamicQueryResultFormatterRegistry formatterRegistry,
        Map<String, ExtensionFactory> factories)
    {
        return new ExtensionRegistry(() -> catalog, formatterRegistry, _ -> {
            // Ignore trackers.
        }, factories);
    }
}
