package nl.ulso.curator.server;

import nl.ulso.curator.query.OutputFormat;

import java.util.Optional;

public record Result(Status status, OutputFormat outputFormat, Optional<String> output)
{
    public static Result ok(OutputFormat outputFormat, String output)
    {
        return new Result(Status.OK, outputFormat, Optional.of(output));
    }

    public static Result clientError(String errorMessage)
    {
        return new Result(Status.CLIENT_ERROR, OutputFormat.MARKDOWN, Optional.of(errorMessage));
    }

    public static Result notFound(String errorMessage)
    {
        return new Result(Status.NOT_FOUND, OutputFormat.MARKDOWN, Optional.of(errorMessage));
    }

    public static Result serverError(String errorMessage)
    {
        return new Result(Status.SERVER_ERROR, OutputFormat.MARKDOWN, Optional.of(errorMessage));
    }
}
