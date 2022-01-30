package nl.ulso.obsidian.watcher.vault;

import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * Container for a list of {@link Fragment}s.
 */
abstract class FragmentContainer
        extends LineContainer
        implements Iterable<Fragment>
{
    private final List<Fragment> fragments;

    FragmentContainer(List<String> lines, List<Fragment> fragments)
    {
        super(lines);
        this.fragments = Collections.unmodifiableList(requireNonNull(fragments));
    }

    public List<Fragment> fragments()
    {
        return fragments;
    }

    Fragment fragment(int index)
    {
        return fragments.get(index);
    }

    @Override
    public Iterator<Fragment> iterator()
    {
        return fragments.iterator();
    }
}
