package nl.ulso.curator.extension;

/// Lifecycle callbacks for the parts of an [Extension] that hold resources, like background
/// threads.
///
/// Extensions are created whenever the configuration changes, but they are not necessarily
/// activated: an extension can be dropped because a dependency is missing, for example. That's
/// why constructors must not acquire resources. Instead, bind an implementation of this interface
/// into the set of lifecycles of the extension, qualified with [ExtensionBinding], and acquire
/// resources in [#start()].
///
/// - [#start()] is called after the extension is activated, before the change processors of the
/// extension run for the first time.
/// - [#stop()] is called when the extension is unloaded, after the next set of extensions has been
/// activated.
///
/// Both are called on the curator thread. They must not call
/// [nl.ulso.curator.change.ExternalChangeHandler] synchronously; do that from a separate thread.
public interface ExtensionLifecycle
{
    default void start()
    {
    }

    default void stop()
    {
    }
}
