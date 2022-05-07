package nl.ulso.macu.curator;

public interface CuratorFactory
{
    String name();

    Curator createCurator();
}
