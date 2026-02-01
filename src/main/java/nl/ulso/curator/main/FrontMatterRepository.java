package nl.ulso.curator.main;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.FrontMatterCollector;
import nl.ulso.dictionary.MutableDictionary;
import nl.ulso.curator.vault.*;
import nl.ulso.dictionary.Dictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

import static java.util.Collections.unmodifiableMap;
import static nl.ulso.dictionary.Dictionary.mutableDictionary;

/// Repository that keeps track of all custom front matter for documents in the vault; it
/// combines the [FrontMatterCollector] and [FrontMatterRewriteResolver] interfaces, bridging the
/// collecting of updates to the writing of updates.
@Singleton
final class FrontMatterRepository
    implements FrontMatterCollector, FrontMatterRewriteResolver
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FrontMatterRepository.class);

    private final Vault vault;
    private final Map<String, MutableDictionary> documentFrontMatters;

    @Inject
    public FrontMatterRepository(Vault vault)
    {
        this.vault = vault;
        documentFrontMatters = new HashMap<>();
    }

    @Override
    public void updateFrontMatterFor(
        Document document, Consumer<MutableDictionary> dictionaryConsumer)
    {
        var documentName = document.name();
        var dictionary =
            documentFrontMatters.computeIfAbsent(documentName, _ -> mutableDictionary());
        dictionaryConsumer.accept(dictionary);
        if (dictionary.isEmpty())
        {
            documentFrontMatters.remove(documentName);
        }
    }

    @Override
    public Map<Document, Dictionary> resolveFrontMatterRewrites()
    {
        var finder = new FrontMatterRewriteFinder();
        vault.accept(finder);
        LOGGER.debug("Found {} documents that require an update to their front matter.",
            finder.newFrontMatters.size()
        );
        return unmodifiableMap(finder.newFrontMatters);
    }

    private class FrontMatterRewriteFinder
        extends BreadthFirstVaultVisitor
    {
        private final Map<Document, Dictionary> newFrontMatters = new HashMap<>();

        @Override
        public void visit(Document document)
        {
            var documentName = document.name();
            var newFrontMatter = computeNewFrontMatter(document.frontMatter(),
                documentFrontMatters.get(documentName)
            );
            if (newFrontMatter != null)
            {
                newFrontMatters.put(document, newFrontMatter);
            }
        }

        private Dictionary computeNewFrontMatter(Dictionary original, Dictionary updates)
        {
            if (updates == null || updates.isEmpty())
            {
                return cleanUpOriginalFrontMatter(original);
            }
            var updatedPropertyNames = updates.propertyNames().stream().sorted().toList();
            if (isUpdateVoid(original, updates, updatedPropertyNames))
            {
                return null;
            }
            return createNewDictionary(original, updates, updatedPropertyNames);
        }

        // If no front matter updates exist, but the original front matter contains the list
        // of generated keys, then remove this list.
        private Dictionary cleanUpOriginalFrontMatter(Dictionary original)
        {
            if (original.hasProperty(PROPERTY_GENERATED_KEYS))
            {
                var newDictionary = Dictionary.mutableDictionary(original);
                newDictionary.removeProperty(PROPERTY_GENERATED_KEYS);
                return newDictionary;
            }
            return null;
        }

        // If all updates have the same values as their originals, then the update is void and
        // nothing needs to be done.
        private boolean isUpdateVoid(
            Dictionary original, Dictionary updates,
            List<String> updatedPropertyNames)
        {
            for (String updatedPropertyName : updatedPropertyNames)
            {
                var originalProperty = original.getProperty(updatedPropertyName);
                var updatedProperty = updates.getProperty(updatedPropertyName);
                if (!originalProperty.equals(updatedProperty))
                {
                    return false;
                }
                var generatedKeys = original.listOfStrings(PROPERTY_GENERATED_KEYS);
                if (!updatedPropertyNames.equals(generatedKeys))
                {
                    return false;
                }
            }
            return true;
        }

        // If there are updates, then create new front matter, as a copy of the original, with the
        // updates applied, and an extra property that lists all the updates keys.
        private MutableDictionary createNewDictionary(
            Dictionary original, Dictionary updates, List<String> updatedPropertyNames)
        {
            var newDictionary = Dictionary.mutableDictionary(original);
            for (String updatedPropertyName : updatedPropertyNames)
            {
                updates.getProperty(updatedPropertyName).ifPresent(property ->
                    newDictionary.setProperty(updatedPropertyName, property));
            }
            newDictionary.setProperty(PROPERTY_GENERATED_KEYS, updatedPropertyNames);
            return newDictionary;
        }
    }
}
