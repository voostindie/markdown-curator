package nl.ulso.markdown_curator;

public class MusicCuratorFactory implements CuratorFactory
{
    @Override
    public String name()
    {
        return "music";
    }

    @Override
    public Curator createCurator()
    {
        return new MusicCurator();
    }
}
