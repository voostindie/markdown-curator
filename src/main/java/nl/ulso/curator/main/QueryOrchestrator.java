package nl.ulso.curator.main;

import nl.ulso.curator.change.Changelog;

import java.util.Set;

/// Runs the relevant queries in the vault and returns the resulting document updates.
///
/// The relevant queries are all those queries that are impacted by the changelog or that are
/// in the same documents as those impacted queries.
interface QueryOrchestrator
{
    Set<DocumentUpdate> runFor(Changelog changelog);
}
