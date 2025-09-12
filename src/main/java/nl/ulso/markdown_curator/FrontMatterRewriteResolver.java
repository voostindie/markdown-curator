package nl.ulso.markdown_curator;

import nl.ulso.markdown_curator.vault.*;

import java.util.Map;

/**
 * Resolves front matter to be written to disk based from the existing front matter in documents,
 * combined with the front matter collected by {@link DataModel}s in the
 * {@link FrontMatterUpdateCollector}.
 * <p/>
 * When front matter is overridden, an additional front matter property
 * {@value PROPERTY_GENERATED_KEYS} is added, listing each of the overridden values. This key
 * is used by the system internally, to keep track of changes to be written to disk. But, it's also
 * useful for the user, to know which values will be overridden by the system and therefore are not
 * sensible to change by hand.
 */
public interface FrontMatterRewriteResolver
{
    /**
     * Name of the property used to store the list of keys that have been generated.
     */
    String PROPERTY_GENERATED_KEYS = "generated_keys";

    Map<Document, Dictionary> resolveFrontMatterRewrites();
}
