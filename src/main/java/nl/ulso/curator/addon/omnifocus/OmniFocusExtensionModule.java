package nl.ulso.curator.addon.omnifocus;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import nl.ulso.curator.extension.ExtensionFactory;
import nl.ulso.jxa.JavaScriptForAutomationModule;

/// Makes OmniFocus available as an extension named `omnifocus`. The
/// [nl.ulso.curator.CuratorModule] includes this module; enable the extension in the configuration
/// document, together with the `project` extension it requires.
///
/// OmniFocus is accessed through JavaScript for Automation, which the curator provides; this
/// module includes the [JavaScriptForAutomationModule] for that reason.
///
/// @see OmniFocusExtensionFactory
@Module(includes = JavaScriptForAutomationModule.class, subcomponents = OmniFocusExtension.class)
public abstract class OmniFocusExtensionModule
{
    @Binds
    @IntoMap
    @StringKey("omnifocus")
    abstract ExtensionFactory bindOmniFocusExtensionFactory(OmniFocusExtensionFactory factory);
}
