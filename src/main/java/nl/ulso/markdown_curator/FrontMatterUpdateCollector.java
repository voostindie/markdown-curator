package nl.ulso.markdown_curator;

import nl.ulso.markdown_curator.vault.*;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Collects updates to front matter from {@link DataModel}s.
 * <p/>
 * Front matter is ideally <em>derived</em> from content, and not maintained separately, nor
 * duplicated.
 * <p/>
 * Inject a {@link FrontMatterUpdateCollector} in your {@link DataModel}, and use it to manage
 * custom front matter for documents. When the {@link Curator} runs, it updates the front matter
 * of these documents on disk, when needed.
 */
public interface FrontMatterUpdateCollector
{
    Optional<Dictionary> frontMatterFor(Document document);

    void updateFrontMatterFor(Document document, Consumer<MutableDictionary> dictionaryConsumer);
}
