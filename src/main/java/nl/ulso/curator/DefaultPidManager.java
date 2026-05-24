package nl.ulso.curator;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.util.Collection;

import static java.lang.Character.isDigit;
import static java.lang.Character.isLetter;
import static java.lang.System.getProperty;
import static java.lang.System.lineSeparator;
import static java.nio.file.Files.writeString;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.util.stream.Collectors.joining;
import static org.slf4j.LoggerFactory.getLogger;

final class DefaultPidManager
    implements PidManager
{
    private static final Logger LOGGER = getLogger(PidManager.class);

    private static final String PID_FOLDER = getProperty("java.io.tmpdir");
    private static final String PID_PREFIX = "markdown-curator-";
    private static final String PID_SUFFIX = ".pid";

    private final Path pidFolder;
    private final boolean deleteOnExit;

    DefaultPidManager()
    {
        this(FileSystems.getDefault(), true);
    }

    DefaultPidManager(FileSystem fileSystem, boolean deleteOnExit)
    {
        this.pidFolder = fileSystem.getPath(PID_FOLDER);
        this.deleteOnExit = deleteOnExit;
    }

    @Override
    public boolean anyPidExists(Collection<CuratorFactory> curatorFactories)
    {
        for (var curatorFactory : curatorFactories)
        {
            var path = pidPathFor(curatorFactory.name());
            if (Files.exists(path, NOFOLLOW_LINKS))
            {
                LOGGER.debug("PID file exists for curator '{}'", curatorFactory.name());
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean createPidFor(CuratorFactory curatorFactory)
    {
        var path = pidPathFor(curatorFactory.name());
        try
        {
            writeString(path, ProcessHandle.current().pid() + lineSeparator(), CREATE_NEW);
        }
        catch (IOException e)
        {
            LOGGER.debug("Could not create PID for curator '{}'", curatorFactory.name(), e);
            return false;
        }
        // We can't use path.toFile().deleteOnExit() unconditionally, because the JimFS filesystem
        // used in tests doesn't support `toFile`.
        if (deleteOnExit)
        {
            path.toFile().deleteOnExit();
        }
        return true;
    }

    private Path pidPathFor(String curatorName)
    {
        return pidFolder.resolve(PID_PREFIX + cleanup(curatorName) + PID_SUFFIX);
    }

    private String cleanup(String name)
    {
        return name.chars()
            .filter(c -> isLetter(c) || isDigit(c))
            .mapToObj(c -> String.valueOf((char) c))
            .map(String::toLowerCase)
            .collect(joining());
    }
}
