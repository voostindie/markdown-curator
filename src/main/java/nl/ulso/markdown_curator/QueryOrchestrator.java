package nl.ulso.markdown_curator;

import java.util.Set;

/// Runs the relevant queries in the vault and returns the resulting document updates.
///
/// "relevant" for now means: all queries in the vault.
public interface QueryOrchestrator
{
    Set<DocumentUpdate> runFor(Changelog changelog);
}
