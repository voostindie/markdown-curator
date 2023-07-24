
This document is used for testing: all queries are run after which it is persisted (in memory). The output must then equal the content of the [[queries-expected]] document.

<!--query:toc-->
- Albums
- Members
- Recordings
- Backlinks
- No-op
- Timeline
<!--/query (fc7d18f8)-->

## Albums

<!--query:albums
artist: Marillion
-->
- [[An Hour Before It's Dark]], 2022
- [[F E A R]], 2016
<!--/query (891f5b16)-->

## Members

<!--query:members
artist: Marillion
-->
- [[Ian Mosley]]
- [[Mark Kelly]]
- [[Pete Trewavas]]
- [[Steve Hogarth]]
- [[Steve Rothery]]
<!--/query (6d89f6e9)-->

## Recordings

<!--query:recordings
song: Be Hard On Yourself
-->
- Track 1 on [[An Hour Before It's Dark]]
<!--/query (ea7d2944)-->

## Backlinks

<!--query:backlinks
document: Marillion
-->
- [[An Hour Before It's Dark]]
    - [[An Hour Before It's Dark#About|About]]
- [[F E A R]]
    - [[F E A R#About|About]]
- [[Ian Mosley]]
    - [[Ian Mosley#About|About]]
- [[Mark Kelly]]
    - [[Mark Kelly#About|About]]
- [[Pete Trewavas]]
    - [[Pete Trewavas#About|About]]
- [[Steve Hogarth]]
    - [[Steve Hogarth#About|About]]
- [[Steve Rothery]]
    - [[Steve Rothery#About|About]]
<!--/query (71b5d8c5)-->

## No-op

<!--query:noop-->
Content placed inside this query is left intact, because the query returns a
"no-op". This allows queries to basically turn themselves off in some cases,
without overwriting what was previously produced.
<!--/query-->

## Timeline

<!--query:timeline
document: "An Hour Before It's Dark"
-->
- **[[2023-01-28]]**:
    - Listened to [[An Hour Before It's Dark]] once more.

(*1 result*)
<!--/query (4c5b8da2)-->
