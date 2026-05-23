package nl.ulso.curator.addon.omnifocus;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.*;

@Singleton
final class ResourceBundleOmniFocusMessages
    implements OmniFocusMessages
{
    private final ResourceBundle bundle;

    @Inject
    ResourceBundleOmniFocusMessages(Optional<Locale> optionalLocale)
    {
        var locale = optionalLocale.orElse(Locale.ENGLISH);
        this.bundle = ResourceBundle.getBundle("OmniFocus", locale);
    }

    @Override
    public String allGoodTitle()
    {
        return bundle.getString("allGoodTitle");
    }

    @Override
    public String allGoodText()
    {
        return bundle.getString("allGoodText");
    }

    @Override
    public String projectsWithoutDocumentsTitle()
    {
        return bundle.getString("projectsWithoutDocumentsTitle");
    }

    @Override
    public String documentsWithoutProjectsTitle()
    {
        return bundle.getString("documentsWithoutProjectsTitle");
    }

    @Override
    public String createProjectInOmniFocus()
    {
        return bundle.getString("createProjectInOmniFocus");
    }
}
