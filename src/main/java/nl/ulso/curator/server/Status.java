package nl.ulso.curator.server;

public enum Status
{
    OK(200),
    CLIENT_ERROR(400),
    NOT_FOUND(404),
    NOT_ACCEPTABLE(406),
    SERVER_ERROR(500);

    private final int httpStatusCode;

    Status(int httpStatusCode)
    {
        this.httpStatusCode = httpStatusCode;
    }

    public int httpStatusCode()
    {
        return httpStatusCode;
    }
}
