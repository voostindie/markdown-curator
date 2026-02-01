package nl.ulso.curator.journal;

import java.util.*;

import static java.util.Collections.unmodifiableList;

/**
 * Represents a nested list of lines into a tree, to compute the line values.
 * <p/>
 * Imagine a nested list of items as a tree. Now stretch a rubber band around the tree and let go.
 * The rubber band snaps back and snugly fits the tree. Then, starting from the top, follow the
 * band around the tree, and incrementally number it. Once you're done you each node in the tree
 * will have two numbers: one to the left, one to the right. Given these numbers it's very easy
 * to compute the parents and children of any node, in linear time.
 * <p/>
 * In this system, the Outline is only the means to compute the line values - the left and right
 * values of each node, or line. Once those values have been computed, the outline is destructed
 * again.
 */
class Outline
{
    private static final int TAB_SIZE = 4;
    private static final char SPACE = ' ';
    private static final char TAB = '\t';
    private final Outline parent;
    private final List<Outline> children;
    private final int depth;
    private int left;
    private int right;

    private Outline(Outline parent)
    {
        this.parent = parent;
        this.depth = parent == null ? 0 : parent.depth + 1;
        this.children = new ArrayList<>();
        this.left = -1;
        this.right = -1;
    }

    static Outline newOutline(List<String> lines)
    {
        var root = new Outline(null);
        var activeNode = root;
        var activeDepth = 0;
        for (String line : lines)
        {
            var depth = computeDepth(line);
            while (depth <= activeDepth && activeNode != root)
            {
                activeNode = activeNode.parent;
                activeDepth--;
            }
            activeDepth = depth;
            activeNode = activeNode.addChild();
        }
        root.computeLineValues(0);
        return root;
    }

    List<LineValues> toLineValues()
    {
        var list = new ArrayList<LineValues>();
        appendLineInfoTo(list);
        list.removeFirst();
        return unmodifiableList(list);
    }

    Outline addChild()
    {
        var node = new Outline(this);
        children.add(node);
        return node;
    }

    private int computeLineValues(int counter)
    {
        left = counter;
        for (Outline child : children)
        {
            counter = child.computeLineValues(counter + 1);
        }
        right = counter + 1;
        return right;
    }

    private static int computeDepth(String line)
    {
        var spaces = 0;
        var tabs = 0;
        for (var i = 0; i < line.length(); i++)
        {
            char c = line.charAt(i);
            if (c == SPACE)
            {
                spaces++;
            }
            else if (c == TAB)
            {
                tabs++;
            }
            else
            {
                break;
            }
        }
        return tabs + (spaces / TAB_SIZE);
    }

    private void appendLineInfoTo(List<LineValues> list)
    {
        list.add(new LineValues(left, right, depth));
        for (Outline node : children)
        {
            node.appendLineInfoTo(list);
        }
    }

    record LineValues(int left, int right, int depth)
    {
        @Override
        public String toString()
        {
            return "(" + left + ", " + right + ") at depth " + depth;
        }

        public boolean includesInSummary(LineValues other)
        {
            return isChildOf(other) || isSelected(other) || isParentOf(other);
        }

        public boolean isDirectChildOf(LineValues other)
        {
            return depth == other.depth + 1 && left > other.left && right < other.right;
        }

        boolean isChildOf(LineValues other)
        {
            return left > other.left && right < other.right;
        }

        private boolean isSelected(LineValues other)
        {
            return this == other;
        }

        private boolean isParentOf(LineValues other)
        {
            return left < other.left && right > other.right;
        }
    }
}
