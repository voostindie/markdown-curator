package nl.ulso.macu.curator.rabobank;

import nl.ulso.macu.curator.Curator;
import nl.ulso.macu.curator.CuratorFactory;

public class RabobankNotesCuratorFactory
        implements CuratorFactory
{
    @Override
    public String name()
    {
        return "Rabobank";
    }

    @Override
    public Curator createCurator()
    {
        return new RabobankNotesCurator();
    }
}
