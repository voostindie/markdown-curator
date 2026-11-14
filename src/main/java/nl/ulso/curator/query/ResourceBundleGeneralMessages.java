package nl.ulso.curator.query;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.util.*;

import static java.util.ResourceBundle.Control.FORMAT_PROPERTIES;
import static java.util.ResourceBundle.Control.getNoFallbackControl;
import static java.util.ResourceBundle.getBundle;


final class ResourceBundleGeneralMessages
    implements GeneralMessages
{
    private static final String BUNDLE_NAME = "GeneralMessages";

    private final Provider<Locale> locale;

    ResourceBundleGeneralMessages()
    {
        this(Locale.US);
    }

    ResourceBundleGeneralMessages(Locale locale)
    {
        this(() -> locale);
    }

    /// The locale can change at runtime, so it's looked up whenever a message is needed.
    @Inject
    ResourceBundleGeneralMessages(Provider<Locale> locale)
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
    public String noResults()
    {
        return bundle().getString("results.none");
    }
}
