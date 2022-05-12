package nl.ulso.markdown_curator.curator.rabobank;

import nl.ulso.markdown_curator.Curator;
import nl.ulso.markdown_curator.CuratorFactory;

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
