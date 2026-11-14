package nl.ulso.curator.addon.backlink;

import java.util.Set;

interface BacklinkRepository
{
    Set<String> backlinksFor(String documentName);
}
