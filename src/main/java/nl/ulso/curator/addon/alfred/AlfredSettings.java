package nl.ulso.curator.addon.alfred;

import java.util.Optional;

public record AlfredSettings(
    Optional<String> projectsPath,
    Optional<String> contactsPath,
    Optional<String> teamsPath,
    Optional<String> thirdPartiesPath,
    Optional<String> remindersPath
)
{
}
