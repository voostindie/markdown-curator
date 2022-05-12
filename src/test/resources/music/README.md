---
aliases: "Start", "Home"
---
## About this vault

This is a sample vault, used in the automated test cases.

This vault is considered to be read-only. It is not actually processed by the tests themselves. Instead, the automated test suite first copies all files into an in-memory filesystem. All tests then run against this in-memory filesystem. 

That way this vault can never become corrupt, nor get in an inconsistent state.

## About the contents

To make the contents of this vault a little bit more interesting it contains a small database of songs, the albums they appear on, and the artists that perform them. It should be mostly self-explanatory.

What this vault tries to show is that information written as Markdown is much more human-friendly than encoding that same data in formats like YAML. Of course *extracting* the data from the Markdown content is a bit harder then, but that's what this tool is all about.

A few lessons for maintainers of (Markdown) wikis:

- **Don't repeat yourself**. Write useful information at one place only, at the place you're most likely to look for it when you need it.
- **Don't maintain lists manually**. Lists of links to other pages in a vault are definitely useful, but maintaining them by hand will eventually lead to inconsistencies.

Using this tool it's possible to take these lessons to heart. To begin with, replace all manual lists with queries. From then on the lists will be generated and updated automatically. Always correct.

## How queries work

Queries all look the same. This is about as short as it gets:

```markdown
<!--query:albums-->
<!--/query-->
```

This statement triggers the (custom) query "albums", which outputs a list of all albums produced by the artist named after the current document.

Once this tool picks up this query, it places its output in between the two HTML comment tags. Like so:

```markdown
<!--query:albums-->
- [[Album]], Year
- [[Album]], Year
<!--/query-->
```

Note that when you (pre)view this page in a Markdown viewer or web browser, the query definition - the stuff embedded within the first HTML tag - is gone from view. All you see is the output. And that's the whole point!

This particular query requires no additional configuration. But it does support it:

```markdown
<!--query:albums
artist: Marillion
reverse: true
-->
<!--/query-->
```

The configuration of a query is just a piece of YAML.

If you do not specify a query, if the query you refer to is not supported, or if the query is configured incorrectly, instead of normal output you'll get an error message:

```markdown
<!--query-->
Queries available in this vault are:

- **albums**: lists all albums by an artist
- **recordings**: lists all recordings of a song
- **members**: lists all members of a band
<!--/query-->
```

This allows you to discover and play around with queries as you see fit. Just save your file and watch the output get updated automatically, provided that the tool is running in the background of course!

It gets even better than that: there's a built-in `help`-query, which you can use to provide you with help on a specific query. For example:

```markdown
<!--query:help
query: albums
-->
**albums**: lists all albums by an artist, newest first.

Configuration options:

- **artist**: Name of the artist. Defaults to document name.
- **reverse**: Whether the list should be reversed. Defaults to false.
```