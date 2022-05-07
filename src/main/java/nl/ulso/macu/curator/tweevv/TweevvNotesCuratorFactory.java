package nl.ulso.macu.curator.tweevv;

import nl.ulso.macu.curator.Curator;
import nl.ulso.macu.curator.CuratorFactory;

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
