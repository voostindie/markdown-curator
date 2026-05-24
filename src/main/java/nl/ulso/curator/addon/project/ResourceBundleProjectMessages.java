package nl.ulso.curator.addon.project;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.*;

import static java.util.ResourceBundle.getBundle;

@Singleton
final class ResourceBundleProjectMessages
    implements ProjectMessages
{
    private final ResourceBundle bundle;

    ResourceBundleProjectMessages()
    {
        this(Optional.empty());
    }

    ResourceBundleProjectMessages(Locale locale)
    {
        this(Optional.of(locale));
    }

    @Inject
    ResourceBundleProjectMessages(Optional<Locale> optionalLocale)
    {
        var locale = optionalLocale.orElse(Locale.ENGLISH);
        this.bundle = getBundle("ProjectMessages", locale);
    }

    @Override
    public String projectLead()
    {
        return bundle.getString("project.lead");
    }

    @Override
    public String projectPriority()
    {
        return bundle.getString("project.priority");
    }

    @Override
    public String projectName()
    {
        return bundle.getString("project.name");
    }

    @Override
    public String projectLastModified()
    {
        return bundle.getString("project.lastModified");
    }

    @Override
    public String projectStatus()
    {
        return bundle.getString("project.status");
    }

    @Override
    public String projectPriorityUnknown()
    {
        return bundle.getString("project.priority.unknown");
    }

    @Override
    public String projectDateUnknown()
    {
        return bundle.getString("project.lead.unknown");
    }

    @Override
    public String projectLeadUnknown()
    {
        return bundle.getString("project.lead.unknown");
    }

    @Override
    public String projectStatusUnknown()
    {
        return bundle.getString("project.status.unknown");
    }
}
