package nl.ulso.curator.vault;

import io.methvin.watcher.DirectoryChangeEvent;

import java.util.Map;

/// Handles [DirectoryChangeEvent]s from the filesystem (from the directory watcher library) and
/// applies them to a [FileSystemFolder]. The interface is sealed, and the map
/// [#DIRECTORY_CHANGE_EVENT_HANDLERS] contains all supported event handlers for different file
/// system item types and event types.
sealed interface DirectoryChangeEventHandler
    permits
        DirectoryCreatedEventHandler,
        DirectoryDeletedEventHandler,
        FileCreatedEventHandler,
        FileModifiedEventHandler,
        FileDeletedEventHandler
{
    enum FileSystemItemType
    {
        DIRECTORY,
        FILE
    }

    Map<FileSystemItemType, Map<DirectoryChangeEvent.EventType, DirectoryChangeEventHandler>>
        DIRECTORY_CHANGE_EVENT_HANDLERS = Map.of(
            FileSystemItemType.DIRECTORY, Map.of(
                DirectoryChangeEvent.EventType.CREATE, new DirectoryCreatedEventHandler(),
                DirectoryChangeEvent.EventType.DELETE, new DirectoryDeletedEventHandler()
            ),
            FileSystemItemType.FILE, Map.of(
                DirectoryChangeEvent.EventType.CREATE, new FileCreatedEventHandler(),
                DirectoryChangeEvent.EventType.MODIFY, new FileModifiedEventHandler(),
                DirectoryChangeEvent.EventType.DELETE, new FileDeletedEventHandler()
            )
    );

    void handle(
        DirectoryChangeEvent event,
        FileSystemFolder parent,
        VaultChangedCallback callback
    );
}
