package nl.ulso.curator.addon.omnifocus;

import dagger.*;
import dagger.Module;
import dagger.multibindings.*;
import jakarta.inject.Singleton;
import nl.ulso.curator.addon.project.ProjectAttributeDefinition;
import nl.ulso.curator.addon.project.ProjectModule;
import nl.ulso.curator.change.ChangeProcessor;
import nl.ulso.curator.query.Query;
import nl.ulso.curator.statistics.MeasurementTracker;
import nl.ulso.jxa.JavaScriptForAutomationModule;

import static nl.ulso.curator.addon.omnifocus.OmniFocusProjectAttributeValueProducer.OMNIFOCUS_URL_ATTRIBUTE;
import static nl.ulso.curator.addon.project.ProjectAttributeDefinition.newAttributeDefinition;

/// Module that extends the [ProjectModule] with functionality related to
/// [OmniFocus](https://www.omnigroup.com/omnifocus).
///
/// This module:
///
/// - Adds a project attribute `priority`, that reflects the priority of the project in OmniFocus.
/// - Adds a project attribute `omnifocus`, that links to the matching project in OmniFocus.
/// - Adds a query `omnifocus` that lists the differences between the vault and OmniFocus.
///
/// Both project attributes are published to the project front matter, but they can of course be
/// used elsewhere too.
///
/// Projects are fetched from OmniFocus at startup, and then every few minutes. If you don't see a
/// change in OmniFocus reflected in the vault yet, give it a few minutes!
///
/// This module expects that:
///
/// - All projects for a specific curator live in a folder in OmniFocus. For example, I have 3
/// folders in OmniFocus, one for each area of responsibility, and I have as many curators.
/// - Projects match on name.
///
/// The module has one unsatisfied dependency: an instance of [OmniFocusSettings]. Bind this in your
/// own curator module.
@Module(includes = {JavaScriptForAutomationModule.class, ProjectModule.class})
public abstract class OmniFocusModule
{
    @Binds
    abstract OmniFocusDatabase bindOmniFocusDatabase(DefaultOmniFocusDatabase database);

    @Binds
    abstract OmniFocusRepository bindOmniFocusRepository(DefaultOmniFocusRepository repository);

    @Binds
    abstract OmniFocusMessages bindOmniFocusMessages(ResourceBundleOmniFocusMessages messages);

    @Provides
    @Singleton
    @IntoMap
    @StringKey(OMNIFOCUS_URL_ATTRIBUTE)
    static ProjectAttributeDefinition provideOmniFocusUrl()
    {
        return newAttributeDefinition(String.class, OMNIFOCUS_URL_ATTRIBUTE);
    }

    @Binds
    @IntoSet
    abstract ChangeProcessor bindOmniFocusInitializer(OmniFocusInitializer initializer);

    @Binds
    @IntoSet
    abstract ChangeProcessor bindOmniFocusAttributeProducer(
        OmniFocusProjectAttributeValueProducer producer);

    @Binds
    @IntoSet
    abstract MeasurementTracker bindOmniFocusTracker(
        OmniFocusProjectAttributeValueProducer tracker);

    @Binds
    @IntoSet
    abstract Query bindOmniFocusQuery(OmniFocusQuery omniFocusQuery);
}
