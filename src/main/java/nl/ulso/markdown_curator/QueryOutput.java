package nl.ulso.markdown_curator;

import nl.ulso.markdown_curator.vault.QueryBlock;

record QueryOutput(QueryBlock queryBlock, String content, String hash, boolean isChanged) {}
