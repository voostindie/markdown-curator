package nl.ulso.markdown_curator.journal;

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
    private int left;
    private int right;

    private Outline(Outline parent)
    {
        this.parent = parent;
        this.children = new ArrayList<>();
        this.left = -1;
        this.right = -1;
    }

    static Outline newOutline(List<String> lines)
    {
        var root = new Outline(null);
        var activeNode = root;
        var activeIndent = 0;
        for (String line : lines)
        {
            var indent = computeIndent(line);
            while (indent <= activeIndent && activeNode != root)
            {
                activeNode = activeNode.parent;
                activeIndent--;
            }
            activeIndent = indent;
            activeNode = activeNode.addChild();
        }
        root.computeLineValues(0);
        return root;
    }

    List<LineValues> toLineValues()
    {
        var list = new ArrayList<LineValues>();
        appendLineInfoTo(list);
        list.remove(0);
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

    private static int computeIndent(String line)
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
        list.add(new LineValues(left, right));
        for (Outline node : children)
        {
            node.appendLineInfoTo(list);
        }
    }

    record LineValues(int left, int right)
    {
        @Override
        public String toString()
        {
            return "(" + left + ", " + right + ")";
        }

        public boolean includesInSummary(LineValues other)
        {
            return isChildOf(other) || isSelected(other) || isParentOf(other);
        }

        private boolean isChildOf(LineValues other)
        {
            return left > other.left() && right < other.right();
        }

        private boolean isSelected(LineValues other)
        {
            return this == other;
        }

        private boolean isParentOf(LineValues other)
        {
            return left < other.left() && right > other.right();
        }
    }
}
