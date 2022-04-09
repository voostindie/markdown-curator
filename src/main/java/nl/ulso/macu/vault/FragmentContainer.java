package nl.ulso.macu.vault;

import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * Container for a list of {@link Fragment}s.
 */
abstract class FragmentContainer
        extends LineContainer
{
    private final List<Fragment> fragments;

    FragmentContainer(List<String> lines, List<Fragment> fragments)
    {
        super(lines);
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
