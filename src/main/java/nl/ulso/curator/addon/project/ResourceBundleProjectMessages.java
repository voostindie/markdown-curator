package nl.ulso.curator.addon.project;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import nl.ulso.curator.extension.ExtensionScope;

import java.util.*;

import static java.util.ResourceBundle.Control.FORMAT_PROPERTIES;
import static java.util.ResourceBundle.Control.getNoFallbackControl;
import static java.util.ResourceBundle.getBundle;


@ExtensionScope
final class ResourceBundleProjectMessages
    implements ProjectMessages
{
    private static final String BUNDLE_NAME = "ProjectMessages";

    private final Provider<Locale> locale;

    ResourceBundleProjectMessages()
    {
        this(Locale.US);
    }

    ResourceBundleProjectMessages(Locale locale)
    {
        this(() -> locale);
    }

    /// The locale can change at runtime, so it's looked up whenever a message is needed.
    @Inject
    ResourceBundleProjectMessages(Provider<Locale> locale)
    {
        this.locale = locale;
    }

    /// Looks up the bundle for the current locale. Bundles are cached by the JVM. They don't fall
    /// back to the default locale of the JVM; if there is no bundle for the locale, the base
    /// bundle, in English, applies.
    private ResourceBundle bundle()
    {
        return getBundle(BUNDLE_NAME, locale.get(), getNoFallbackControl(FORMAT_PROPERTIES));
    }

    @Override
    public String projectLead()
    {
        return bundle().getString("project.lead");
    }

    @Override
    public String projectPriority()
    {
        return bundle().getString("project.priority");
    }

    @Override
    public String projectName()
    {
        return bundle().getString("project.name");
    }

    @Override
    public String projectLastModified()
    {
        return bundle().getString("project.lastModified");
    }

    @Override
    public String projectStatus()
    {
        return bundle().getString("project.status");
    }

    @Override
    public String projectPriorityUnknown()
    {
        return bundle().getString("project.priority.unknown");
    }

    @Override
    public String projectDateUnknown()
    {
        return bundle().getString("project.lead.unknown");
    }

    @Override
    public String projectLeadUnknown()
    {
        return bundle().getString("project.lead.unknown");
    }

    @Override
    public String projectStatusUnknown()
    {
        return bundle().getString("project.status.unknown");
    }
}
