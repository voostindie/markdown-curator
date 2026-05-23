package nl.ulso.curator.addon.omnifocus;

import dagger.*;
import dagger.Module;
import dagger.multibindings.*;
import jakarta.inject.Singleton;
import nl.ulso.curator.addon.project.ProjectAttributeDefinition;
import nl.ulso.curator.addon.project.ProjectSettings;
import nl.ulso.curator.change.ChangeProcessor;
import nl.ulso.curator.change.ExternalChangeHandler;
import nl.ulso.curator.query.Query;
import nl.ulso.curator.statistics.MeasurementTracker;
import nl.ulso.jxa.JavaScriptForAutomation;

import java.util.Locale;

import static nl.ulso.curator.addon.omnifocus.OmniFocusProjectAttributeValueProducer.OMNIFOCUS_URL_ATTRIBUTE;
import static nl.ulso.curator.addon.project.ProjectAttributeDefinition.newAttributeDefinition;

@Module
abstract class OmniFocusTestModule
{
    @Binds
    abstract ExternalChangeHandler bindExternalChangeHandler(
        ExternalChangeHandlerStub externalChangeHandler);

    @Binds
    abstract OmniFocusDatabase bindOmniFocusDatabase(OmniFocusDatabaseStub omniFocusDatabase);

    @Binds
    abstract JavaScriptForAutomation bindJavaScriptForAutomation(
        JavaScriptForAutomationStub javascriptForAutomation);

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

    @BindsOptionalOf
    abstract Locale bindOptionalLocale();

    @Provides
    static ProjectSettings provideProjectSettings()
    {
        return new ProjectSettings("Projects");
    }

    @Provides
    static OmniFocusSettings provideOmniFocusSettings()
    {
        return new OmniFocusSettings("Area");
    }
}
