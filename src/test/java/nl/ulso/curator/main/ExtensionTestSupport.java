package nl.ulso.curator.main;

import nl.ulso.curator.extension.ExtensionFactory;
import nl.ulso.curator.query.DynamicQueryCatalog;

import java.util.Map;

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
        return new ExtensionRegistry(() -> catalog, _ -> {
            // Ignore trackers.
        }, factories);
    }
}
