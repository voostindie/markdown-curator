package nl.ulso.macu.curator.rabobank;

import nl.ulso.macu.vault.Document;

import java.util.Map;

record OrgUnit(Document team, Document parent, Map<String, Document> leadership)
{
}
