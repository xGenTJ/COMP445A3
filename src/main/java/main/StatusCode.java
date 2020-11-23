package main;

public enum StatusCode {
    OK(200, "OK"),
    CREATED(201, "Created"),
    BAD_REQUEST(400, "Bad Request"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found");

    public final int code;
    public final String phrase;

    private StatusCode(int code, String phrase) {
        this.code = code;
        this.phrase = phrase;
    }
}
