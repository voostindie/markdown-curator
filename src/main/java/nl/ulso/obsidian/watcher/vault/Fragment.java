package nl.ulso.obsidian.watcher.vault;

import java.util.List;

/**
 * A fragment is any distinguishable piece of content in a document. For now that is limited
 * to a document's front matter, sections (of any level), and simple text. Every fragment is
 * visitable (see {@link Visitor}.
 */
public sealed interface Fragment
        permits FrontMatter, Section, Text
{
    List<String> lines();

    String content();

    void accept(Visitor visitor);
}
