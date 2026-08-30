package com.ats.fitcertificate.exception;

/** Raised when an external dependency (Claude API, embeddings) fails. */
public class UpstreamException extends RuntimeException {
    public UpstreamException(String msg) { super(msg); }
    public UpstreamException(String msg, Throwable cause) { super(msg, cause); }
}
