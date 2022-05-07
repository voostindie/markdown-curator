package nl.ulso.macu.curator.personal;

import nl.ulso.macu.curator.Curator;
import nl.ulso.macu.curator.CuratorFactory;

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
