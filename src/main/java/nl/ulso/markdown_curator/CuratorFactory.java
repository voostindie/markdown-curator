package nl.ulso.markdown_curator;

/**
 * Curators must implement this interface.
 */
public interface CuratorFactory
{
    /**
     * @return The name of this curator; used extensively in logging
     */
    String name();

    /**
     * Create the curator; don't implement this method yourself, instead leave it up to Dagger.
     *
     * @return The actual curator.
     */
    Curator createCurator();
}
