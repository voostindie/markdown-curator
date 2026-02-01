package nl.ulso.curator.vault;

import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * Container for a list of {@link Fragment}s.
 */
abstract class FragmentContainer
        extends FragmentBase
{
    private final List<Fragment> fragments;

    FragmentContainer(List<Fragment> fragments)
    {
        this.fragments = unmodifiableList(requireNonNull(fragments));
    }

    public List<Fragment> fragments()
    {
        return fragments;
    }

    Fragment fragment(int index)
    {
        return fragments.get(index);
    }
}
