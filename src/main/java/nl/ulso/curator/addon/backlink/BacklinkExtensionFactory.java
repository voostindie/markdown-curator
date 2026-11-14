package nl.ulso.curator.addon.backlink;

import jakarta.inject.Inject;
import nl.ulso.curator.extension.Extension;
import nl.ulso.curator.extension.ExtensionContext;
import nl.ulso.curator.extension.ExtensionFactory;
import nl.ulso.dictionary.Dictionary;

/// Creates the backlinks extension, which has no settings and requires no other extensions.
final class BacklinkExtensionFactory
    implements ExtensionFactory
{
    private final BacklinkExtension.Factory factory;

    @Inject
    BacklinkExtensionFactory(BacklinkExtension.Factory factory)
    {
        this.factory = factory;
    }

    @Override
    public String description()
    {
        return "Lists the documents that link to a document, through the `backlinks` query.";
    }

    @Override
    public Extension create(Dictionary configuration, ExtensionContext context)
    {
        return factory.create();
    }
}
