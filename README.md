# Macu - Markdown Curator

TL;DR:

- Map your Obsidian documents to meaningful concepts
- Extract knowledge from your concepts
- Integrate new-found knowledge in your documents

Okay, that probably tells you very little.

This is a daemon application that monitors one or more Obsidian Vaults. Based on changes happening to a vault, it runs queries, refreshes result sets and generates and updates files in the vault.

In this very first version of this application there is no configuration. Everything is "as code", built into a single "monolithic" application. Because this thing is just for me anyway. Configurability implies complexity. I don't need that, thank you very much.

Most of the things this application does can also be achieved using plugins in Obsidian, like the [Dataview plugin](https://github.com/blacksmithgu/obsidian-dataview). I do not like those kinds of plugins. I believe they defeat Obsidian's purpose. Obsidian is all about storing knowledge in **portable** Markdown files. Sprinkling those same files with code (queries) that only Obsidian with a specific plugin installed can understand is not the right idea, I think.

With this application I have the best of both worlds: portable Markdown and "dynamic" content.

Shouldn't I have built this application as an Obsidian plugin itself? Maybe. Probably. But, I didn't. Why not? Because I'm sure my use of Markdown will outlive my use of Obsidian. Also, being able to change files in a vault with any editor *and* have the functionality of this application still work leads to fewer surprises. 

See the [CHANGELOG](CHANGELOG.md) for releases and the roadmap.
