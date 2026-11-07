package nl.ulso.curator.addon.project;

import dagger.BindsInstance;
import dagger.Subcomponent;
import nl.ulso.curator.extension.Extension;
import nl.ulso.curator.extension.ExtensionModule;
import nl.ulso.curator.extension.ExtensionScope;

import java.util.Map;

/// The project model as an extension; see [ProjectModule].
@ExtensionScope
@Subcomponent(modules = {ExtensionModule.class, ProjectModule.class})
public interface ProjectExtension
    extends Extension
{
    @Subcomponent.Factory
    interface Factory
    {
        ProjectExtension create(
            @BindsInstance ProjectSettings settings,
            @BindsInstance Map<String, ProjectAttributeDefinition> attributeDefinitions);
    }
}
