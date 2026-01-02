package nl.ulso.markdown_curator;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.markdown_curator.vault.*;
import nl.ulso.markdown_curator.vault.Dictionary;

import java.util.*;
import java.util.function.Consumer;

import static java.util.Collections.unmodifiableMap;
import static nl.ulso.markdown_curator.vault.Dictionary.mutableDictionary;

@Singleton
final class FrontMatterCollector
    implements FrontMatterUpdateCollector, FrontMatterRewriteResolver
{
    private final Vault vault;
    private final Map<String, MutableDictionary> documentFrontMatters;

    @Inject
    public FrontMatterCollector(Vault vault)
    {
        this.vault = vault;
        documentFrontMatters = new HashMap<>();
    }

    @Override
    public Optional<Dictionary> frontMatterFor(Document document)
    {
        return Optional.ofNullable(documentFrontMatters.get(document.name()));
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
