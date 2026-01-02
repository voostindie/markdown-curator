package nl.ulso.markdown_curator;

import nl.ulso.markdown_curator.vault.*;

import java.util.Optional;
import java.util.function.Consumer;

/// Collects updates to front matter from [ChangeProcessor]s.
///
/// Front matter is ideally _derived_ from content and not maintained separately nor duplicated.
///
/// Inject a [FrontMatterUpdateCollector] in your [ChangeProcessor] and use it to manage custom
/// front matter for documents. When the [Curator] runs, it updates the front matter of these
/// documents on disk, when needed.
public interface FrontMatterUpdateCollector
{
    Optional<Dictionary> frontMatterFor(Document document);

    void updateFrontMatterFor(Document document, Consumer<MutableDictionary> dictionaryConsumer);
}
