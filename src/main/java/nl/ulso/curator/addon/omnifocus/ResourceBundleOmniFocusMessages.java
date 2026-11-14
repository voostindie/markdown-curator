package nl.ulso.curator.addon.omnifocus;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import nl.ulso.curator.extension.ExtensionScope;

import java.util.*;

import static java.util.ResourceBundle.Control.FORMAT_PROPERTIES;
import static java.util.ResourceBundle.Control.getNoFallbackControl;
import static java.util.ResourceBundle.getBundle;

@ExtensionScope
final class ResourceBundleOmniFocusMessages
    implements OmniFocusMessages
{
    private static final String BUNDLE_NAME = "OmniFocus";

    private final Provider<Locale> locale;

    ResourceBundleOmniFocusMessages(Locale locale)
    {
        this(() -> locale);
    }

    /// The locale can change at runtime, so it's looked up whenever a message is needed.
    @Inject
    ResourceBundleOmniFocusMessages(Provider<Locale> locale)
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
    public String projectsWithoutDocumentsTitle()
    {
        return bundle().getString("projectsWithoutDocumentsTitle");
    }

    @Override
    public String documentsWithoutProjectsTitle()
    {
        return bundle().getString("documentsWithoutProjectsTitle");
    }

    @Override
    public String createProjectInOmniFocus()
    {
        return bundle().getString("createProjectInOmniFocus");
    }
}
