# One daemon process for all vaults

## Status

Adopted

## Decision

The application will run in a single daemon process, independent of the amount of vaults to monitor.

## Context

I have multiple vaults on my machine. They are truly independent of one another. A straightforward approach is to have one process per vault. But because of the choice for Java and the JVM (see ADR0001), this is not the best approach.  

## Consequences

## Options considered

1. **One daemon for all vaults**
2. One daemon per vault
3. LaunchAgents and equivalents

### One daemon for all vaults

#### Pros

- Only one JVM running at a time. Only one process to manage.

#### Cons

- A bit more code needed to maintain multiple independent vaults.
- Guaranteeing that code for a vault runs completely isolated is hard, if not impossible. (Unless I introduce something like OSGi.)

### One daemon per vault 

#### Pros

- One vault, one process. Everything is nicely isolated.

#### Cons

- Multiple processes to manage.
- Multiple JVMs running at the same time.

### LaunchAgents and equivalents

Note: [this is what I currently do, with a Ruby program.](https://github.com/voostindie/obsidian-reports). I have a LaunchAgent per vault, monitoring the filesystem and triggering the program to run.{

#### Pros

- Uses platform-native methods to monitor the filesystem.
- The application runs only when something relevant happens.

#### Cons

- All files in a vault need to be read into memory, every time.
- Startup costs of the application are incurred every time a file changes. 
- Extra configuration to manage, outside of the application.
