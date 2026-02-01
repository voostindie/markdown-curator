package nl.ulso.curator;

import java.util.Set;

/// Runs the relevant queries in the vault and returns the resulting document updates.
///
/// The relevant queries are all those queries that are impacted by the changelog or that are
/// in the same documents as those impacted queries.
public interface QueryOrchestrator
{
    Set<DocumentUpdate> runFor(Changelog changelog);
}
