package nl.ulso.curator.change;

/// Processes a change for something that happened outside the system.
///
/// In the curator, the vault is watching for changes to folders and documents and triggers changes
/// whenever that happens. To trigger a change from another source - a clock, an external database,
/// a webservice, or whatever an application needs - inject this interface in the application and
/// call its [#process(Change)] method.
public interface ExternalChangeHandler
{
    void process(Change<?> change);
}
