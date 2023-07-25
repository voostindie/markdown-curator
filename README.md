[![Java CI with Maven](https://github.com/voostindie/markdown-curator/actions/workflows/maven.yml/badge.svg)](https://github.com/voostindie/markdown-curator/actions/workflows/maven.yml)
[![CodeQL](https://github.com/voostindie/markdown-curator/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/voostindie/markdown-curator/actions/workflows/codeql-analysis.yml)

[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=voostindie_markdown-curator&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=voostindie_markdown-curator)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=voostindie_markdown-curator&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=voostindie_markdown-curator)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=voostindie_markdown-curator&metric=coverage)](https://sonarcloud.io/summary/new_code?id=voostindie_markdown-curator)

# Markdown Curator

## TL;DR

- This is a Java 20+ library and small application framework for processing directories of Markdown documents.
- It is especially well suited for Obsidian vaults and iA Writer libraries.
- It detects queries in the documents, runs them, and writes back the results.
- As an application, it monitors and processes directories in the background.

Okay, that probably doesn't tell you much.

## The 5-minute introduction

This is a Java library and application framework that spins up a daemon. This daemon monitors one or more directories of Markdown documents, like [Obsidian](https://obsidian.md) vaults or [iA Writer](https://ia.net/writer) libraries. Based on changes happening in the directories, it detects and runs queries embedded in the documents, generates Markdown output for these queries and embeds this output in the documents themselves.

Here's an example of what you can write in a Markdown document:

```
<!--query:list
folder: Articles
-->
THE OUTPUT WILL GO HERE
<!--/query-->
```

Put this snippet (without the code block) in a document in a directory tracked by this tool, save it and watch `THE OUTPUT WILL GO HERE` be magically replaced with a sorted list of links to documents in the `Articles` subdirectory. Add a new article there, delete one, or update an existing one, and watch the list get updated instantly.

The query syntax may seem a bit weird at first, but notice that it is built up of HTML comment tags. That means that the query definitions disappear from view when you preview the Markdown, or export it to some other format using the tool of your choice, leaving you with just the query output. In effect the query syntax is invisible to any tool that processes Markdown correctly.

What can a query do? Whatever you can code in Java! The internal API of this tool allows you to extract any kind of information from your documents or elsewhere and use them in queries.

Don't know which queries are available? Simply put a blank query in a document in a vault and save it:

```
<!--query-->
<!--/query-->
```

By default, this tool provides just a couple of built-in generic queries. See the section on those below for more details. These are useful queries, but to make this tool really shine, you will want to create your own queries. 

To use this library, you have to code your own Java application, define this tool as a dependency, and implement your own curator, custom data models and custom queries. See further on for an example.

The [music](src/test/resources/music/README.md) test suite provides examples of what this tool can do and how it works. The test code contains a [MusicCuratorModule](src/test/java/nl/ulso/markdown_curator/MusicCuratorModule.java) that can serve as an example for building your own curator, on top of your own vault.

[Vincent's Markdown Curator](https://github.com/voostindie/vincents-markdown-curator) (vmc) is my own, personal implementation that I use every day. It runs on top of 3 independent vaults - work, volunteering, personal - each with their own unique queries, and some shared across. You might find some inspiration in it.

## Obsidian users: Ye be warned!

If you're an Obsidian user, then note that most of the things this library does can also be achieved using plugins, like [Dataview](https://github.com/blacksmithgu/obsidian-dataview). I do not like those kinds of plugins. I believe they defeat Obsidian's purpose. For me Obsidian is all about storing knowledge in *portable* Markdown files. Sprinkling those same files with code (queries) that only Obsidian with a specific plugin installed can understand is not the right idea, I think.

With this library I have the best of both worlds: portable Markdown and "dynamic" content. Query output is embedded in the documents as Markdown content. As far as Obsidian concerns, this tool is not even there. 

On the other hand, Obsidian plugins are much easier to install and use. This library requires you to get your hands dirty with Java. You must build your own  application. That's not for everyone.

Shouldn't I have built this library as an Obsidian plugin itself? Maybe. Probably. But, I didn't. Why not? Because I'm sure my use of Markdown will outlive my use of Obsidian. Also, being able to change files in a vault with any editor *and* have this library still work in the background leads to fewer surprises.

Case in point: with the June 2022 release of [iA Writer 6](https://ia.net/writer) and its support for wikilinks, mostly compatible with Obsidian's, there's now a truly native and focused macOS app for personal knowledge management. iA Writer has far fewer bells and whistles than Obsidian, but that's *exactly* why I happen to like it so much. And because I do not depend on Obsidian-specific plugins for my content, I can easily switch between them at will and even use them simultaneously.

## "Vincent Flavored Markdown"

This tool is specifically written for a variant of Markdown that I call *Vincent Flavored Markdown*. Basically VFM is the same as [Github Flavored Markdown (GFM)](https://github.github.com/gfm/) with the following constraints and additions:

- A document can have YAML front matter, between `---`.
- For headers only ATX (`#`) headers are supported, without the optional closing sequence of `#`s. Setext-style headers are not supported.
- Headers are always aligned to the left margin.
- Code blocks are always surrounded with backticks, not indented.
- Internal links - links to other documents in the same repository - use double square brackets. (`[[Like this]]`). The link always points to a file name within the repository. (This is what Obsidian and iA Writer do.)
- File names are considered to be globally unique within the repository.    Surprises might happen otherwise.
- The document's title is, in this order of preference:
	- The title of the first level 1 header, if present and at the top of the document.
	- The value of the YAML front matter field `title`, if present
	- The file name, without extension.
- The file extension is `.md`.
- Queries can be defined in HTML comments, for this tool to process. See below.

In practice I only use level 1 headers or the `title` property if the filename is not a good title. In 99% of the cases it is. I do not duplicate the filename inside the document, because, well, that's duplication.

If these limitations are not to your liking, then feel free to send me a pull request to support your own personal preferences.

## Not a Markdown parser!

This library/application does **not** fully parse Markdown. It only does so on a line-by-line level. Documents are broken up in blocks of:

- Front matter
- Sections (these can be nested)
- Code
- Queries
- Text

A text block is "anything *not* of the above". The content of a text block itself is not parsed. Whether text is in bold or italic, is in a list or in a table, uses CriticMarkup or some other extension: it's all oblivious to the internal parser; it's all just text. When you build your own queries, it's up to you to extract content out of the various blocks, as you see fit. 

I have some ideas to extend this further in order to make query construction easier, but I'm not planning on introducing a full Markdown parser.

## Creating your own application

Creating your own application means that you'll need to:

- Create a new Java artifact
- Create and publish a custom curator
- Create and register one or more queries
- Create your own custom data models

### Create a new Java artifact

- Copy the `template-application` in this repository to a new directory.
- Update the `pom.xml` in your copy:
  - Set your own groupId and artifactId.
  - Make sure to use the latest version of dependencies and plugins.

A `mvn clean package` and `java -jar target/my-markdown-curator.jar` should result in the application starting up and exiting immediately, telling you that it can't find any curators.

### Create and publish a custom curator

- Extend the `CuratorModule` base class.
- Add your implementation to `src/main/resources/META-INF/services/nl.ulso.markdown_curator.CuratorModule`.

A `mvn clean package` and `java -jar target/my-markdown-curator.jar` should result in the application starting up and staying up, monitoring the directory you provided in your own custom curator.

Try changing a file in any Markdown document in your document repository now. For example, add the `toc` query. Magic should happen!

    <!--query:toc-->
    <!--/query-->

### Create and register one or more queries

- Implement the `Query` interface.
- Register the query in your `CuratorModule` subclass, with `registerQuery`.

Rebooting your application should result in the availability of the new query.

### Create your own custom data models

Once you've implemented a couple of queries you might run into one or two issues:

1. **Duplication**. Extracting specific values from documents might be complex, and might be needed across queries.
2. **Heavy processing**. Running many queries across large data sets on every change, no matter how small, can be CPU intensive.

To solve these issues you can create your own data models, which you can then build your queries upon.

To do so, implement the `DataModel` interface, register it in your curator module and share it with your own queries. Whenever a change is detected, the curator requests your data models to update themselves accordingly, through the `vaultChanged` method. 

**IMPORTANT**: make sure your data models are registered as `@Singleton`s!

By extending the `DataModelTemplate` class you get full refreshes basically for free, and an easy way to process events in a more granular fashion, if so desired: simply override the `process` methods of choice and provide your own implementation.

As an example of a custom data model, see the built-in `JournalModel`, which is used by the `timeline` query to generate Logseq-like summaries.

## Built-in queries

This section lists all built-in queries and explains what they do. Each query supports arguments to be passed to it. To know what they are, use the `help` query somewhere in a monitored repository. For example:

```
<!--query:help
name: timeline
-->
<!--/query-->
```

This will write information on the selected query (in this case: `timeline`), including the parameters it supports as the query output.

### `deadlinks`

This is an optional query. To use it in your own vault, install the `LinksModule` in your Curator module.

This query lists all dead links in a document. In other words: all links that refer to documents that do not exist within the vault.

### `list`

This query generates a sorted list of documents in a folder. Each item is a link to a document. Through the configuration the list can be reverse sorted, and documents in subfolders can be recursively added as well.

### `timeline`

This is an optional query. To use it in your own vault, install the `JournalModule` in your Curator module. 

The timeline query is inspired by [Logseq](https://logseq.com). I like the way Logseq puts emphasis on the daily log as the place to write notes. What I do not like about Logseq, however:

- Everything is an outline. I prefer the freedom full Markdown gives me. When I write an article for example, I use sections and paragraphs, without bullets.
- It's all dynamic, and therefore the functionality only works in Logseq itself. I like to be able to use any text editor.
- All documents are stored in the same folder. I prefer using a couple of folders to categorize documents: "Projects", "Contacts", "Articles", and so on.

The timeline query solves that, by generating static timelines from the daily logs. 

The `JournalModule` requires two configuration values on construction:

1. The name of folder in which daily entries are stored. `Journal` is a good choice. This folder is expected to contain files in the format `<YYYY-mm-DD>.md`. (This is what Obsidian uses by default.)
2. The name of the level-2 section in each daily entry that contains the outline. I use `Activities` myself. Journal entries are extracted only from this section.

### `toc`

This query generates a table of contents for the current document. You can tweak the table by configuring the minimum and maximum header levels to include.

### `table`

This query generates a sorted table of pages, with optional front matter fields in additional columns. This is a more powerful version of the `list` query.

Through the configuration you can extract any front-matter field from the individual documents and add them to the table. Next to that you can (reverse) sort the table on any front-matter field.

## FAQ

### Help! Changes to some files are not detected!

Solution: set the environment to use the right language, e.g. `LC_CTYPE=UTF-8`.

My personal experience:

- When run from the command line, changes to all files were detected.
- When run from within IntelliJ IDEA, changes to files with emoj's in their name where not detected.

Adding the `LC_CTYPE` variable to the Run configuration environment fixed it. The command line already had it.

### The application exits immediately after "Instantiating curator"

Solution: apply the `--enable-preview` JVM setting.

The latest version of this library uses virtual threads, introduced in JDK 19 as a preview. I tried implementing a fallback method, but that only made the application hang if `--enable-preview` is not provided.

Anyway, virtual threads seem to work really well! My own curator is running 900+ queries in parallel, in less than 20 milliseconds.

### How do I force a query to re-generate its output?

This shouldn't be needed, but when in doubt, it's easy: remove or change the hash at the bottom of the query definition. 

The curator uses the hash to detect changes in query output; not the query output itself. It does this because query output is not actually stored in memory. That's why you see these hashes show up in the query output (as part of the closing HTML comment).

When you change the hash in any way, the curator will assume the content has changed, and will replace it with a fresh query result.
