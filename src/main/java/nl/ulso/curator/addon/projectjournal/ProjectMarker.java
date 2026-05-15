package nl.ulso.curator.addon.projectjournal;

import nl.ulso.curator.addon.journal.Marker;

import java.util.*;

/// Base class for markers used in the project journal to resolve attribute values from.
///
/// A project marker is just a normal marker, except that when the marker is used with a specific
/// alias, it marks a line to extract a project attribute value from.
///
/// The set of aliases to trigger on is specified in the front matter of the marker itself.
abstract class ProjectMarker
{
    private final Marker marker;
    private final Map<String, String> markdownLinks;

    ProjectMarker(Marker marker)
    {
        this.marker = marker;
        var map = new HashMap<String, String>();
        var aliases = marker.document().frontMatter().listOfStrings(frontMatterProperty());
        for (String alias : aliases)
        {
            var link = "[[" + marker.name() + "|" + alias + "]]";
            map.put(link, alias);
        }
        this.markdownLinks = Map.copyOf(map);
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == null || getClass() != object.getClass())
        {
            return false;
        }
        ProjectMarker other = (ProjectMarker) object;
        return Objects.equals(marker, other.marker);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(marker);
    }

    protected String name()
    {
        return marker.name();
    }

    /// Returns a map of links to aliases supported by the project marker. The links are the actual
    /// text as used in the project journal, e.g. `[[marker|alias]]`. The aliases are the
    /// corresponding alias.
    protected Map<String, String> markdownLinks()
    {
        return markdownLinks;
    }

    /// Returns the name of the front matter property used to specify aliases for this marker.
    abstract String frontMatterProperty();

    @Override
    public String toString()
    {
        return marker.name();
    }
}

