package nl.ulso.curator.addon.omnifocus;

import dagger.BindsInstance;
import dagger.Subcomponent;
import nl.ulso.curator.addon.project.ProjectAttributeDefinition;
import nl.ulso.curator.addon.project.ProjectRepository;
import nl.ulso.curator.extension.Extension;
import nl.ulso.curator.extension.ExtensionModule;
import nl.ulso.curator.extension.ExtensionScope;

import java.util.Map;

/// OmniFocus as an extension; see [OmniFocusModule].
@ExtensionScope
@Subcomponent(modules = {ExtensionModule.class, OmniFocusModule.class})
public interface OmniFocusExtension
    extends Extension
{
    @Subcomponent.Factory
    interface Factory
    {
        OmniFocusExtension create(
            @BindsInstance OmniFocusSettings settings,
            @BindsInstance ProjectRepository projectRepository,
            @BindsInstance Map<String, ProjectAttributeDefinition> attributeDefinitions);
    }
}
