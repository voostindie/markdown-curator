[![Java CI with Maven](https://github.com/voostindie/markdown-curator/actions/workflows/maven.yml/badge.svg)](https://github.com/voostindie/markdown-curator/actions/workflows/maven.yml)
[![CodeQL](https://github.com/voostindie/markdown-curator/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/voostindie/markdown-curator/actions/workflows/codeql-analysis.yml)

[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=voostindie_markdown-curator&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=voostindie_markdown-curator)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=voostindie_markdown-curator&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=voostindie_markdown-curator)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=voostindie_markdown-curator&metric=coverage)](https://sonarcloud.io/summary/new_code?id=voostindie_markdown-curator)

# Markdown Curator

See the [CHANGELOG](CHANGELOG.md) for releases and the roadmap.

## TL;DR

- This is a Java 17+ library and small application framework for processing directories of Markdown documents.
- It is especially well suited for Obsidian vaults.
- It detects queries in the documents, runs them, and writes back the results.
- As an application, it monitors and processes directories in the background.

Okay, that probably doesn't tell you much.

## Obsidian users: Ye be warned!

If you're an Obsidian user, then note that most of the things this library does can also be achieved using plugins, like [Dataview](https://github.com/blacksmithgu/obsidian-dataview). I do not like those kinds of plugins. I believe they defeat Obsidian's purpose. For me Obsidian is all about storing knowledge in *portable* Markdown files. Sprinkling those same files with code (queries) that only Obsidian with a specific plugin installed can understand is not the right idea, I think.

With this library I have the best of both worlds: portable Markdown and "dynamic" content. Query output is embedded in the documents as Markdown content. As far as Obsidian concerns, this tool is not even there. 

On the other hand, Obsidian plugins are much easier to install and use. This library requires you to get your hands dirty with Java. You must build your own Java application. That's not for everyone.

Shouldn't I have built this library as an Obsidian plugin itself? Maybe. Probably. But, I didn't. Why not? Because I'm sure my use of Markdown will outlive my use of Obsidian. Also, being able to change files in a vault with any editor *and* have this library still work in the background leads to fewer surprises.

## The 5-minute introduction

This is a Java library and application framework that can spin up a daemon. This daemon can monitor one or more directories of Markdown documents, like [Obsidian](https://obsidian.md) vaults. Based on changes happening in the directories, it detects and runs queries embedded in the documents, generates Markdown output for these queries and embeds this output in the documents themselves.

Here's an example of what you can write in a Markdown document:

```
<!--query:list
folder: Articles
-->
THE OUTPUT WILL GO HERE
<!--/query>
```

Put this snippet (without the code block) in a document in a directory tracked by this tool, save it and watch `THE OUTPUT WILL GO HERE` be magically replaced with a sorted list of links to documents in the `Articles` subdirectory. Add a new article there, delete one, or update an existing one, and watch the list get updated instantly.

The query syntax may seem a bit weird at first, but notice that it is built up of HTML comment tags. That means that the query definitions disappear from view when you preview the Markdown, or export it to some other format using the tool of your choice, leaving you with just the query output. In effect the query syntax invisible to any tool that processes Markdown correctly.

Whatever can you put in a query? Whatever you can come up with and code in Java. The internal API of this tool allows you to extract any kind of information from your documents or elsewhere and use them in queries.

Don't know which queries are available? Simply put a blank query in your content and save it:

```
<!--query-->
<!--/query-->
```

By default, this tool provides just a couple of built-in generic queries: `list`, `table` and `toc`. To make this tool really useful, you will want to create your own queries. 

To use this library, you have to configure your own application, define this tool as a dependency, and code your own curator and custom queries. See further on for an example.

The [music](src/test/resources/music/README.md) test suite provides examples of what this tool can do and how it works. The test code contains a [MusicCurator](src/test/java/nl/ulso/markdown_curator/MusicCurator.java) that can serve as an example for building your own curator, on top of your own vault.

## Getting started

- Create a new Java artifact
- Create and publish a custom curator.
- Create and register one or more queries.

## Create a new Java artifact

- Copy the `template-application` in this repository to a new directory.
- Update the `pom.xml` in your copy:
  - Set your own groupId and artifactId.
  - Make sure to use the latest version of dependencies and plugins.

A `mvn clean package` and `java -jar target/my-markdown-curator.jar` should result in the application starting up and exiting immediately, telling you that it can't find any curators.

## Create and publish a custom curator

- Implement the `Curator` interface, by subclassing the `CuratorTemplate` class.
- Implement the `CuratorFactory` interface.
- Add your implementation to `src/main/resources/META-INF/services/nl.ulso.markdown_curator.CuratorFactory`.

A `mvn clean package` and `java -jar target/myproject.jar` should result in the application starting up and staying up, monitoring the directory you provided in your own custom `Curator`.

Try changing a file in any Markdown document in your document repository now. For example, add the `toc` query. Magic should happen!

    <!--query:toc-->
    <!--/query-->

## Create and register one or more queries

- Implement the `Query` interface.
- Register the query in your `CuratorTemplate` subclass, in `registerQueries`.

Rebooting your application should result in the availability of the new queries.
