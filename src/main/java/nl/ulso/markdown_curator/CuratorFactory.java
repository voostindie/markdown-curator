package nl.ulso.markdown_curator;

public interface CuratorFactory
{
    String name();

    Curator createCurator();
}
