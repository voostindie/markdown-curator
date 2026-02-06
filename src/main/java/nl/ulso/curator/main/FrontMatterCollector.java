package nl.ulso.curator.main;

import nl.ulso.curator.change.ChangeProcessor;
import nl.ulso.curator.vault.Document;
import nl.ulso.dictionary.MutableDictionary;

import java.util.function.Consumer;

/// Collects updates to front matter from [ChangeProcessor]s.
///
/// Front matter is ideally _derived_ from content and not maintained separately nor duplicated.
///
/// Inject a [FrontMatterCollector] in your [ChangeProcessor] and use it to manage custom front
/// matter for documents. When the curator runs, it updates the front matter of these documents on
/// disk, when needed.
public interface FrontMatterCollector
{
    /// Updates the front matter of the given document; the front matter to update of that document
    /// is passed as a mutable dictionary, allowing multiple changes to the front matter at once.
    void updateFrontMatterFor(Document document, Consumer<MutableDictionary> dictionaryConsumer);
}
