package nl.ulso.curator.addon.omnifocus;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import nl.ulso.curator.extension.ExtensionFactory;
import nl.ulso.jxa.JavaScriptForAutomation;

/// Registers the OmniFocus extension like the [OmniFocusExtensionModule] does, but with stubs for
/// everything the extension needs from the curator.
@Module(subcomponents = OmniFocusExtension.class)
abstract class OmniFocusTestModule
{
    @Binds
    abstract JavaScriptForAutomation bindJavaScriptForAutomation(
        JavaScriptForAutomationStub javascriptForAutomation);

    @Binds
    @IntoMap
    @StringKey("omnifocus")
    abstract ExtensionFactory bindOmniFocusExtensionFactory(OmniFocusExtensionFactory factory);
}
