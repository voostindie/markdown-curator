package nl.ulso.markdown_curator.curator.tweevv;

import nl.ulso.markdown_curator.Curator;
import nl.ulso.markdown_curator.CuratorFactory;

public class TweevvNotesCuratorFactory
        implements CuratorFactory
{
    @Override
    public String name()
    {
        return "TweeVV";
    }

    @Override
    public Curator createCurator()
    {
        return new TweevvNotesCurator();
    }
}
