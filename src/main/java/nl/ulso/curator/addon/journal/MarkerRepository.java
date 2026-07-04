package nl.ulso.curator.addon.journal;

import nl.ulso.dictionary.Dictionary;

import java.util.Map;

/// Keeps track of marker journal entries.
interface MarkerRepository
{
    Map<String, Marker> markers();

    Dictionary markerSettings(String markerName);
}
