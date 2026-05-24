package nl.ulso.curator.query;

import jakarta.inject.Inject;

import java.util.*;

import static java.util.ResourceBundle.getBundle;

final class ResourceBundleGeneralMessages
    implements GeneralMessages
{
    private final ResourceBundle bundle;

    ResourceBundleGeneralMessages()
    {
        this(Optional.empty());
    }

    ResourceBundleGeneralMessages(Locale locale)
    {
        this(Optional.of(locale));
    }

    @Inject
    ResourceBundleGeneralMessages(Optional<Locale> optionalLocale)
    {
        var locale = optionalLocale.orElse(Locale.ENGLISH);
        this.bundle = getBundle("GeneralMessages", locale);
    }

    @Override
    public String noResults()
    {
        return bundle.getString("results.none");
    }
}
