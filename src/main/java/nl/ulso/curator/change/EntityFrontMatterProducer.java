package nl.ulso.curator.change;

import nl.ulso.curator.main.FrontMatterCollector;
import nl.ulso.curator.vault.Document;
import nl.ulso.dictionary.MutableDictionary;

/// Base class for change processors that produce custom front matter for entities.
public abstract class EntityFrontMatterProducer<E>
    extends EntityProcessor<E>
{
    private final FrontMatterCollector frontMatterCollector;

    protected EntityFrontMatterProducer(FrontMatterCollector frontMatterCollector)
    {
        this.frontMatterCollector = frontMatterCollector;
    }

    @Override
    protected final void entityCreated(E newEntity, ChangeCollector collector)
    {
        processFrontMatter(newEntity);
    }

    @Override
    protected final void entityUpdated(E oldEntity, E newEntity, ChangeCollector collector)
    {
        processFrontMatter(newEntity);
    }

    @Override
    protected final void entityDeleted(E oldEntity, ChangeCollector collector)
    {
        // Nothing to do here. When a document is deleted, so is its custom front matter.
    }

    private void processFrontMatter(E entity)
    {
        var document = resolveDocumentFrom(entity);
        frontMatterCollector.updateFrontMatterFor(document, dictionary ->
            processFrontMatter(entity, dictionary)
        );
    }

    /// Resolve the document that the front matter should be written to.
    protected abstract Document resolveDocumentFrom(E entity);

    /// Extract front matter from the entity and write it to the dictionary.
    ///
    /// **Important**: always set all front matter properties for the entity. If a front matter
    /// property is optional, then explicitly delete it from the dictionary!
    protected abstract void processFrontMatter(E entity, MutableDictionary dictionary);
}
