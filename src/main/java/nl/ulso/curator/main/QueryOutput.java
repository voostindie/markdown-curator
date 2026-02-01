package nl.ulso.curator.main;

import nl.ulso.curator.vault.QueryBlock;

record QueryOutput(QueryBlock queryBlock, String content, String hash, boolean isChanged) {}
