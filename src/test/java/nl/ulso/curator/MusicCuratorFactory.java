package nl.ulso.curator;

public class MusicCuratorFactory
        implements CuratorFactory
{
    @Override
    public String name()
    {
        return "music";
    }

    @Override
    public Curator createCurator()
    {
        return DaggerMusicCurator.create().curator();
    }
}
