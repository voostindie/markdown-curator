[![Java CI with Maven](https://github.com/voostindie/markdown-curator/actions/workflows/maven.yml/badge.svg)](https://github.com/voostindie/markdown-curator/actions/workflows/maven.yml)
[![CodeQL](https://github.com/voostindie/markdown-curator/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/voostindie/markdown-curator/actions/workflows/codeql-analysis.yml)

[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=voostindie_markdown-curator&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=voostindie_markdown-curator)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=voostindie_markdown-curator&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=voostindie_markdown-curator)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=voostindie_markdown-curator&metric=coverage)](https://sonarcloud.io/summary/new_code?id=voostindie_markdown-curator)

# Markdown Curator

## TL;DR

- This is a Java 25+ library and small application framework for processing directories of Markdown documents.
- It is especially well suited for [Obsidian](https://obsidian.md) vaults and [iA Writer](https://ia.net/writer) libraries. More generally: for Markdown-based wikis that use wikilinks to link to other documents in the same wiki.
- It detects queries in the documents, executes them, and writes back the results in the documents themselves.
- It extracts information out of documents as properties and writes them into documents as front matter.
- As an application, it monitors and processes directories in the background.

Even shorter: this tool allows you to keep your static content [DRY](https://en.wikipedia.org/wiki/Don%27t_repeat_yourself).

## The 5-minute introduction

This is a Java library and application framework that spins up a daemon. This daemon monitors one or more wikis: directories of Markdown documents like [Obsidian](https://obsidian.md) vaults or [iA Writer](https://ia.net/writer) libraries. Based on changes happening in the directories and the Markdown documents, it detects and runs queries embedded in the documents, generates Markdown output for these queries and embeds this output in the documents themselves. Also, it can update the front matter in the documents with properties derived from the content.

Here's an example of what you can write in a Markdown document:

```markdown
<!--query:list
folder: Articles
-->
THE OUTPUT WILL GO HERE
<!--/query-->
```

Put this snippet (without the code block) in a document in a directory tracked by this tool, save it and watch `THE OUTPUT WILL GO HERE` be magically replaced with a sorted list of links to documents in the `Articles` subdirectory. Add a new article there, delete one, or update an existing one, and watch the list get updated instantly. 

> Or, I should say: after 3 seconds. This is the built-in delay for processing files after changes are detected. This delay is needed for Obsidian, which automatically saves files every few seconds when you're typing in them. If in the space of the 3-second delay another change is detected, the scheduled processing run is aborted, and a new one is scheduled to run, again in 3 seconds. This little trick ensures the processing doesn't happen unnecessarily often.  

The query syntax may seem a bit weird at first, but notice that it is built up of HTML comment tags. That means that the query definitions disappear from view when you preview the Markdown, or when you export it to some other format using the tool of your choice, leaving you with just the query output. In effect, the query syntax is invisible to any tool that processes Markdown correctly.

What can a query do? Whatever you can code in Java! The internal API of this tool allows you to extract any kind of information from your documents or elsewhere and use them in queries.

Don't know which queries are available? Put a blank query in a document in a vault and save it:

```markdown
<!--query-->
<!--/query-->
```
After the curator has kicked in, you'll see an overview of all queries available.

By default, this tool provides just a couple of built-in generic queries. See the section on those below for more details. These are useful queries, but to make this tool really shine, you will want to create your own queries.

This tool also provided optional queries, in separate modules. By plugging in these modules into your application and providing some necessary configuration, you instantly gain additional queries to use.

To use this system, you have to code your own Java application, define this tool as a dependency, and implement your own curator, custom change processors, and custom queries. See further on for an example.

The [markdown-demo-curator](https://github.com/voostindie/markdown-curator-demo) provides a simple example of a repository of notes and an application on top of it to monitor it for changes and process queries.

The [template-application](template-application/) folder holds a minimal template for creating your own curator.

[Vincent's Markdown Curator](https://github.com/voostindie/vincents-markdown-curator) (vmc) is my own, personal implementation that I use every day. It runs on top of 4 independent vaults - work, volunteering, personal, demo - each with their own unique queries, and some shared across. You might find some inspiration in it.

## Extracting information from content to front matter properties

When maintaining notes in my personal knowledge system, I try to follow two rules:

1. Never duplicate information manually.
2. Make all relevant, meaningful information part of the content itself. 

The first rule is actually why this tool exists. With it, content can be sliced and diced in any way conceivable and then represented back as part of the content itself, automatically. Change the original content, and all its transformations follow. No mistakes are possible.

The second rule exists because I believe information should be in human-readable plain text (Markdown), and not forced in some kind of structure to satisfy some tool. Tools should adapt to people, not the other way around. I do not consider front matter in a document to be part of the content of that document. It's metadata. In my opinion, metadata should be only one of two things:

1. **Operational data**: data that supports some tool in doing its work. Examples: "Use this CSS template for the HTML export", or "Use this image as a cover for the cards view", or "Allow these aliases for the document". Things like that.
2. **Derived data**: data that is extracted from the content and persisted in an easier digestible way for tools. Examples: "The status of a project" or "The author of a book" or "The poster of a movie".

Front matter has its use. For example, Obsidian plugins like [Bases](https://help.obsidian.md/bases) allow Notion-like datasets to be created with it conveniently. But, there's a danger in them also, because they can either lead to crucial information being duplicated - both in the content and in the properties - or moved out of the content into the metadata, to exist only there. I'm not certain which is worse.

This is why the curator can update front matter automatically. The content - the Markdown - is where the source information is. Front matter properties should not replace or manually duplicate source information. But they can be derived from it. Both rules are followed, and tools that act on front matter are satisfied at the same time.

(In Obsidian, I prefer to hide document properties. I don't need to see them, because everything I might need is in the content. But I do use them.)

## Obsidian users: Ye be warned!

If you're an Obsidian user, then note that most of the things this library does can also be achieved using plugins, like [Dataview](https://github.com/blacksmithgu/obsidian-dataview). I do not like those kinds of plugins. I believe they defeat Obsidian's purpose. For me Obsidian is all about storing knowledge in *portable* Markdown files. Sprinkling those same files with code (queries) that only Obsidian with specific plugins installed can understand is not the right idea, I think.

With this library I get the best of both worlds: portable Markdown and "dynamic" content. Query output is embedded in the documents as Markdown content. As far as Obsidian is concerned, this tool is not even there. 

On the other hand, Obsidian plugins are much easier to install and use. This library requires you to get your hands dirty with Java. You must build your own application. That's not for everyone.

Shouldn't I have built this library as an Obsidian plugin itself? Maybe. Probably. But I didn't. Why not? Because I'm sure my use of Markdown will outlive my use of Obsidian. Also, being able to change files in a vault with any editor *and* have this library still work in the background leads to fewer surprises.

Case in point: with the June 2022 release of [iA Writer 6](https://ia.net/writer) and its support for wikilinks, mostly compatible with Obsidian's, there's now a truly native and focused macOS app for personal knowledge management. iA Writer has far fewer bells and whistles than Obsidian, but that's *exactly* why I happen to like it so much. And because I do not depend on Obsidian-specific plugins for my content, I can easily switch between them at will and even use them simultaneously.

## "Vincent Flavored Markdown"

This tool is specifically written for a variant of Markdown that I call *Vincent Flavored Markdown*. Basically, VFM is the same as [Github Flavored Markdown (GFM)](https://github.github.com/gfm/) with the following constraints and additions:

- A document can have YAML front matter, between `---`.
- For headers only ATX (`#`) headers are supported, without the optional closing sequence of `#`s. Setext-style headers are not supported.
- Headers are always aligned to the left margin.
- Code blocks are always surrounded with backticks, not indented.
- Internal links (links to other documents in the same repository) use double square brackets. (`[[Like this]]`). The link always points to a file name within the repository. (This is what Obsidian and iA Writer do.)
- File names are considered to be globally unique within the repository. Surprises might happen otherwise.
- The document's title is, in this order of preference:
	- The title of the first level 1 header, if present and at the top of the document.
	- The value of the YAML front matter field `title`, if present.
	- The file name, without extension.
- The file extension is `.md`.
- Queries can be defined in HTML comments for this tool to process. See below.

In practice, I only use level 1 headers or the `title` property if the filename is not a good title. In 99% of the cases it is. I do not duplicate the filename inside the document, because, well, that's duplication.

If these limitations are not to your liking, then feel free to send me a pull request to support your own personal preferences.

## Not a Markdown parser!

This library/application does **not** fully parse Markdown. It only does so on a line-by-line level. Documents are broken up in blocks of:

- Front matter
- Sections (these can be nested on different levels)
- Code
- Queries
- Text

A text block is "anything *not* of the above". The content of a text block itself is not parsed. Whether text is in bold or italic, holds a list or a table, uses CriticMarkup or some other format: the internal parser is oblivious to it; it's all just text. When you build your own queries, it's up to you to extract content out of the various blocks, as you see fit. 

I have some ideas to extend this further to make query construction easier, but I'm not planning on introducing a full Markdown parser.

## Creating your own application

Creating your own application means that you'll need to:

- Create a new Java artifact
- Create and publish a custom curator
- Create and register one or more queries
- Create your own custom change processors

### Create a new Java artifact

- Copy the `template-application` in this repository to a new directory.
- Update the `pom.xml` in your copy:
  - Set your own groupId and artifactId.
  - Make sure to use the latest version of dependencies and plugins.

A `mvn clean package` and `java -jar target/my-markdown-curator.jar` should result in the application starting up and exiting immediately, telling you that it can't find any curators.

> Because I have not published his library to Maven central yet, or anywhere else, you have to install this library in your local repository first. To do so: clone it and do an `mvn install`. 

### Create and publish a custom curator

- Define a [Dagger](https://dagger.dev) `@Module` to set up the context of your curator. In the module include *at least* the `nl.ulso.curator.CuratorModule`.
- Define a Dagger `@Component` that depends on your module and that exposes your `Curator` instance. Typically, this is an interface with just one method.
- Compile your code with Maven or in your IDE. If all was well, you'll end up with extra code generated by Dagger. If you named your component `MyComponent`, there will be a `DaggerMyComponent` now too.
- Implement the `CuratorFactory` interface. In its implementation use the Dagger-generated code to create and return your `Curator` instance. 
- Add your `CuratorFactory` implementation to the file `src/main/resources/META-INF/services/nl.ulso.curator.CuratorModule`.

> See the [markdown-curator-demo](https://github.com/voostindie/markdown-curator-demo) for an example.

An `mvn package` and `java -jar target/my-markdown-curator.jar` should result in the application starting up and staying up, monitoring the directory you provided in your own custom curator.

Try changing a file in any Markdown document in your document repository now. For example, add the `toc` query. Magic should happen!

```markdown
<!--query:toc-->
<!--/query-->
```

### Create and register one or more queries

- Implement the `Query` interface.
- Register the query in your own module, by binding the concrete instance into a `Query` set:

```java
@Binds @IntoSet abstract Query bindMyOwnCustomQuery(MyOwnCustomQuery query);
```

Rebooting your application should result in the availability of the new query.

### Create your own custom change processors

Once you've implemented a couple of queries, you might run into one or two issues:

1. **Duplication**. Extracting specific values from documents might be complex, and the same values might be needed across queries.
2. **Heavy processing**. Running many queries across large data sets on every change, no matter how small, can be CPU intensive.

To solve these issues, you can create your own change processors that can transform incoming changes to the vault into other things like custom data models, which you can then build your queries upon.

To do so, implement the `ChangeProcessor` interface and register it in your curator module:

```java
@Binds @IntoSet abstract ChangeProcessor bindMyOwnCustomProcessor(MyOwnCustomChangeProcessor processor);
```

Whenever a change is detected, the curator executes your processors to handle the incoming change, optionally producing new changes for your own domain objects. After all processors have been executed it runs the queries. 

> The curator *always* runs all queries. It's not (yet) smart enough to detect that a query's output will not change based on the changes processed. That would be a cool feature to add to the system, but it would also be solving a problem that I currently do not have; see the FAQ.

**IMPORTANT**: make sure your change processor are registered as `@Singleton`s!

By extending the `ChangeProcessorTemplate` class you get the choice between full and incremental change processing, and an easy way to split the logic of the various processing in separate methods.

As an example of a custom change processor, see the built-in `Journal` or `ProjectRepository`.

## Query collections

This section lists all readily available queries and explains what they do. Each query supports arguments to be passed to it. To know what they are, use the `help` query somewhere in a monitored repository. For example:

```markdown
<!--query:help
name: timeline
-->
<!--/query-->
```

This will write information on the selected query (in this case: `timeline`), including the parameters it supports as the query output.

### Built-in queries

Built-in queries are always available and cannot be disabled.

#### `list`

This query generates a sorted list of documents in a folder. Each item is a link to a document. Through the configuration the list can be reverse sorted, and documents in subfolders can be recursively added as well.

#### `table`

This query generates a sorted table of pages, with optional front matter fields in additional columns. This is a more powerful version of the `list` query.

Through the configuration you can extract any front matter field from the individual documents and add them to the table. Next to that you can (reverse) sort the table on any front-matter field.

#### `toc`

This query generates a table of contents for the current document. You can tweak the table by configuring the minimum and maximum header levels to include.

### Links module queries

By including the `LinksModule` to your Curator module, the queries in this section become available.

```java
@Module(includes = {CuratorModule.class, LinksModule.class})
abstract class MyCuratorModule
{
    // Your code here
}
```

In large vaults this module takes up quite a bit of memory because it keeps an index of all references between documents, including to non-existent ones. 

#### `deadlinks`

This query lists all dead links in a document. In other words: all links that refer to documents that do not exist within the vault.

### Journal module queries

The Journal module supports [Logseq](https://logseq.com)-like daily outlines and has a number of queries to slice and dice information from these outlines.

I like the way Logseq puts emphasis on the daily log as the place to write notes. What I do not like about Logseq, however:

- Everything is an outline. I prefer the freedom full Markdown gives me. When I write an article, for example, I use sections and paragraphs, without bullets.
- It's all dynamic, and therefore the functionality only works in Logseq itself. I like to be able to use any text editor.
- All documents are stored in the same folder. I prefer using a couple of folders to categorize documents: "Projects", "Contacts", "Articles", and so on.

This module addresses that by generating static timelines from the daily logs. The journal looks only at a specific section in the daily log, where it expects an outline. That means the daily log may contain other content as well.

For example, here is the template I currently use for my daily notes:

```markdown
<!--query:dayNav-->
<!--/query-->

## 🏃 Activities

- *Put your outline here!*

## 🗓️ On the agenda

<!--query:timeline-->
<!--/query-->
...
```

To enable the module in your Curator, you have to include it:  

```java
@Module(includes = {CuratorModule.class, JournalModule.class})
abstract class MyCuratorModule
{
    @Provides
    static JournalSettings journalSettings() 
	{
        return new JournalSettings(
            "Journal",      // Where daily journal pages are kept 
            "Markers",      // Where marker descriptions are kept
            "Activities",   // Name of the section with the outline
            "Projects"      // Where project notes are kept
		);
	}
    
    // Your code here
}
```

#### `timeline`

The `timeline` query generates a timeline on a certain topic; by default, this is the page the timeline query is added to. The timeline is sorted by date, newest first. Each entry for the selected topic contains the context from the daily journal, similar to how Logseq does it.

### `marked`

The `marked` query generates a selection of lines annotated with a specific marker, one section per marker, on a certain topic. By default, the topic is the page the query is added to. Lines in each section are ordered according to the timeline; oldest first. The markers themselves are removed from each line.

A marker is nothing more than a reference to a document. That can be *any* document. The document might not even exist; the functionality still works. Markers are useful to collect specific segments from the timeline and show them prominently in their own section, for example, at the top of a document.

Markers only apply to a topic if they are *exactly* one level lower than the topic itself. This is so you can reuse markers for different topics, even when the topics are nested. 

For example, let's say you have a timeline somewhere that looks like this:

```
- Important meeting on [[Topic 1]].
	- Meeting note 1
	- [[❗️]] Important meeting note 2
	- We also discussed [[Topic 2]].
		- [[❗️]] We shouldn't forget this!
```

If you now put the following query on the page of "Topic 1":

```
<!--query:markers markers: [❗]️-->
<!--/query-->
```

...this query will produce the output:

```
## ❗️

- Important meeting note 2
```

It lists all lines marked with a reference to ❗️ and collects them in a single section. It doesn't show "We shouldn't forget about this!", because the marker on that line applies to Topic 2.

You can change the header titles of the section for each marker in the query output by adding a `title` property to the marker document. This ensures consistency across the vault and simplifies the query definition.

In this example, if you were to define the page `❗️` as follows:

```
---
title: ❗️ Important!
---

(Here it's useful to explain what the marker is used for.)
```

...then the section title in all query outputs would include the text "Important !".

If you want to group the entries for a marker by date in the output, then add the front matter variable `group-by-date` with a value of `true`.

Creating marker documents has more advantages than just being able to influence the query output:

- They're documents like any other, so things like backlinks "just work".
- They can define aliases in their front matter, natively supported by Obsidian.
- They prevent dead links in your vault.
- They allow you to explain what a marker is supposed to be used for, for future reference.
- ...they could even have their own timeline!

#### `period`

The `period` query generates a list of notes from a specific folder that were referenced in a certain period. For example:

```
<!--query:period
start: 2023-09-01
end: 2023-09-30
folder: 'Contacts'
-->
- [[Contact A]]
- [[Contact D]]
- [[Contact M]] 
<!--/query-->
```

This example shows all contacts referenced from the daily log in September 2023, in a sorted list.

#### `weekly`

The `weekly` query is a specialization of the `period` query. It picks a specific week of a specific year. If your weekly notes are named `YYYY Week ww` (e.g. `2023 Week 42`) and you place this query in that note, then no configuration is necessary. It will list all projects referenced in that week.   

#### `dayNav`

Put this query at the top of the daily log pages.

The `dayNav` query generates a set of links to the previous and next daily entry in the journal, as well as to the weekly entry that the day's entry belongs to. It also prints the date as a readable text, like "Sunday, October 1, 2023".

#### `weekNav`

Put this query at the top of the weekly log pages.

The `weekNav` query generates a set of links to the previous and next weekly entry in the journal, as well as to each of the individual entries in the week. It also prints the week as a readable text, like "2023, Week 39".

### Project module queries

The Project module turns one folder in your vault, excluding subdirectories, into a project repository. Every document in the folder represents a project that you want to track.

Apart from being a normal document, a project has a number of additional properties. These come built-in:

- **Priority**: an integer value denoting the priority. The lower the more important. Property name: `priority`.
- **Status**: a simple string; I use emojis myself: 🟢, 🟠, 🛑, ⛔️, ✅, 🗑️. Property name: `status`.  
- **Last modification date**: a date representing when the project was last worked on. Property name: `last_modified`.
- **Lead**: a reference to some other document, typically representing a person. Property name: `lead`.

By default, these properties are read from and written to front matter in the project document, using the properties mentioned above, but their source can be overruled by providing custom plugins to resolve these values from wherever you want. 

(Stay tuned for a couple of those to be introduced soon, or have a look at the `vmc` application, which already has a couple. For example, there's a plugin that pulls the last modification date from the journal, and there's another that resolves the priority from the order of the projects in OmniFocus.)

To enable the module in your Curator, you have to include it:

```java
@Module(includes = {CuratorModule.class, ProjectModule.class})
abstract class MyCuratorModule
{
    @Provides
    static ProjectSettings projectSettings() 
	{
        return new ProjectSettings(
            "Projects"      // Where project notes are kept
		);
	}
    
    // Your code here
}
```

#### `projectlist`

This query generates a list of all active projects. In its simplest form, in `list` format, that's all it does. But switch to the `table` format, and you'll get multiple columns, including the properties mentioned above.

#### `projectlead`

This query is a specialization of the `projectlist` query: it selects only those projects with a specific project lead. The selected lead defaults to the document the query is used in. In other words: stick this query in the document that represents one of your co-workers, and you'll instantly get an overview of projects they're in the lead for; no further configuration needed. 

## FAQ

### Help! Changes to some files are not detected!

Solution: set the environment to use the right language, e.g. `LC_CTYPE=UTF-8`.

My personal experience:

- When run from the command line, changes to all files were detected.
- When run from within IntelliJ IDEA, changes to files with emojis in their name where not detected.

Adding the `LC_CTYPE` variable to the Run configuration environment fixed it. The command line already had it.

### How do I force a query to re-generate its output?

This shouldn't be needed, but when in doubt, it's easy: remove or change the hash at the bottom of the query definition. 

The curator uses the hash to detect changes in query output; not the query output itself. It does this because the query output is not stored in memory in between runs of the curator. That's why you see these hashes show up in the query output (as part of the closing HTML comment).

When you change the hash in any way, the curator will assume the content has changed and will replace it with a fresh query result.

### Can I run the application not as a daemon, but just once, for example in a build pipeline?

Yes, you can! By providing the argument "-1" or "--once" at start-up, the curator will do its work once and then quit.

### How hungry on resources is this tool?

I've gone out of my way to limit both the amount of memory and CPU it uses. This is a Java application, so there's that; there's a minimal memory footprint. But on top of that, I aim to use as little as possible.

However, this application reads *all* Markdown files in memory and keeps them there. Note that this is *excluding* the output of the queries in the content; those are only kept in memory during a processing run. I made this choice explicitly; see ADR00003. This means that the amount of memory used grows with the number and size of documents in a repository. Since those documents are normally written by hand, I figured it would take about forever to use more memory than is available in modern hardware. And by that time the amount of memory will likely have doubled at least. 

When changes are detected, the application kicks in by first executing all available change processors, then running all queries embedded in the content, and finally writing to disk only those files that have changed. The queries run in parallel, using Java virtual threads.

This is all nice and good, but what does it actually mean?

Well, I use [vincents-markdown-curator](https://github.com/voostindie/vincents-markdown-curator) (*vmc*) all day, every day. It sits on top of 4 repositories, with around 5000 Markdown documents in there. My work repository is the largest by far, with over 3700 documents. In those documents there are over 4900 embedded queries. And this number is growing, because I create at least one document for every working day to hold the daily log, and every daily log embeds at least two queries.

I've limited *vmc* to use at most 128 MB of memory. When it's not doing anything, it currently uses a little over 65 MB, according to jconsole. When it's running queries in my work repository, this spikes to about twice that, but then quickly comes down again.

CPU-wise I don't get to see it use more than 5% of my M1 Pro and then only during processing. Otherwise, it's using around 0.3%.

Running all 4900 queries takes around 100 milliseconds.

All in all, I basically don't notice that *vmc* is running all the time. It will be many years from now that I'll have so many documents with so many embedded queries in them that performance becomes an issue. So, that's a challenge for another day. I think that by then I'll have replaced my machine with an M30 or whatever is the norm by then.