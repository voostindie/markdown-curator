# Macu - Markdown Curator

TL;DR:

- Map your Obsidian documents to meaningful concepts
- Extract knowledge from your concepts
- Integrate new-found knowledge in your documents

Okay, that probably tells you very little.

This is a daemon application that monitors one or more Obsidian Vaults. Based on changes happening to a vault, it detects and runs queries, generates results sets and updates files in the vault with these results.

In this very first version of this application there is no configuration. Everything is "as code", built into a single application. Because this thing is just for me anyway. Configurability implies complexity. I don't need that right now, thank you very much.

If this tool evolves the way I hope, it'll grow into some kind of pluggable system, where the core code is separate from the vault-specific code, and where more and more can be done through configuration in a vault itself, instead of code. We'll see. 

Most of the things this application does can also be achieved using plugins in Obsidian, like the [Dataview plugin](https://github.com/blacksmithgu/obsidian-dataview). I do not like those kinds of plugins. I believe they defeat Obsidian's purpose. Obsidian is all about storing knowledge in **portable** Markdown files. Sprinkling those same files with code (queries) that only Obsidian with a specific plugin installed can understand is not the right idea, I think.

With this application I have the best of both worlds: portable Markdown and "dynamic" content.

Shouldn't I have built this application as an Obsidian plugin itself? Maybe. Probably. But, I didn't. Why not? Because I'm sure my use of Markdown will outlive my use of Obsidian. Also, being able to change files in a vault with any editor *and* have this application still work in the background leads to fewer surprises.

To lift a tip of the veil, here's an example of what you can write in a Markdown document:

<!--query:list
folder: Articles
-->
OUTPUT WILL GO HERE
<!--/query>

Put this snippet in a document in an Obsidian vault tracked by this tool, save it and watch the `OUTPUT WILL GO HERE` be magically replaced with a sorted list of links to documents in the `Articles` folder.

The query syntax may seem a bit weird, but notice that is built up of HTML comment tags. That means the queries disappear from view when you preview the Markdown, or export it to some other format, leaving you with just the output.

Whatever can you put in a query? Basically anything, really. Whatever you can come up with and code in Java. The internal API of this tool allows you to easily extract any kind of information from your documents and use them in queries.

See the [music](music/README.md) test suite for examples of what this tool can do and how it works. The test code contains a [MusicCurator](src/test/java/nl/ulso/macu/curator/MusicCurator.java) that can serve as an example for building your own curator, on top of your own vault. 

## Changes

See the [CHANGELOG](CHANGELOG.md) for releases and the roadmap.

## Ideas (let's not call it a roadmap)

In random order:

- Turn `macu` into a standalone application that detects loads plugins as Java modules.
- Support locale-specific output
- Come up with a generic data model description (entities, attributes, relations) on top of which queries can be written in "SQL"
- Allow data models to be defined in configuration instead of Java.
- Build a native image with Graal.
