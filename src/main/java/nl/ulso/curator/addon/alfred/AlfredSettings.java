package nl.ulso.curator.addon.alfred;

public final class AlfredSettings
{
    private final String contactsPath;
    private final String projectsPath;
    private final String remindersPath;
    private final String systemsPath;
    private final String teamsPath;
    private final String thirdPartiesPath;

    private AlfredSettings(Builder builder)
    {
        this.contactsPath = builder.contactsPath;
        this.projectsPath = builder.projectsPath;
        this.remindersPath = builder.remindersPath;
        this.systemsPath = builder.systemsPath;
        this.teamsPath = builder.teamsPath;
        this.thirdPartiesPath = builder.thirdPartiesPath;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    String contactsPath()
    {
        return contactsPath;
    }

    String projectsPath()
    {
        return projectsPath;
    }

    String remindersPath()
    {
        return remindersPath;
    }

    String systemsPath()
    {
        return systemsPath;
    }

    String teamsPath()
    {
        return teamsPath;
    }

    String thirdPartiesPath()
    {
        return thirdPartiesPath;
    }

    public static class Builder
    {
        private String contactsPath;
        private String projectsPath;
        private String remindersPath;
        private String systemsPath;
        private String teamsPath;
        private String thirdPartiesPath;

        public Builder contactsPath(String contactsPath)
        {
            this.contactsPath = contactsPath;
            return this;
        }

        public Builder projectsPath(String projectsPath)
        {
            this.projectsPath = projectsPath;
            return this;
        }

        public Builder remindersPath(String remindersPath)
        {
            this.remindersPath = remindersPath;
            return this;
        }

        public Builder systemsPath(String systemsPath)
        {
            this.systemsPath = systemsPath;
            return this;
        }

        public Builder teamsPath(String teamsPath)
        {
            this.teamsPath = teamsPath;
            return this;
        }

        public Builder thirdPartiesPath(String thirdPartiesPath)
        {
            this.thirdPartiesPath = thirdPartiesPath;
            return this;
        }

        public AlfredSettings build()
        {
            return new AlfredSettings(this);
        }
    }
}
