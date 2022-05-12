package nl.ulso.markdown_curator.curator.personal;

import nl.ulso.markdown_curator.Curator;
import nl.ulso.markdown_curator.CuratorFactory;

public class PersonalNotesCuratorFactory
        implements CuratorFactory
{
    @Override
    public String name()
    {
        return "Personal";
    }

    @Override
    public Curator createCurator()
    {
        return new PersonalNotesCurator();
    }
}
