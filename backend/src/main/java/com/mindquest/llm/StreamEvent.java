package com.mindquest.llm;

/**
 * Event emitted during streaming LLM responses.
 * Contains partial text, completion status, and optional error.
 */
public final class StreamEvent {
    
    private final String requestId;
    private final String partialText;
    private final boolean done;
    private final Throwable error;
    
    public StreamEvent(String requestId, String partialText, boolean done, Throwable error) {
        this.requestId = requestId;
        this.partialText = partialText != null ? partialText : "";
        this.done = done;
        this.error = error;
    }
    
    public static StreamEvent partial(String requestId, String text) {
        return new StreamEvent(requestId, text, false, null);
    }
    
    public static StreamEvent done(String requestId) {
        return new StreamEvent(requestId, "", true, null);
    }
    
    public static StreamEvent error(String requestId, Throwable error) {
        return new StreamEvent(requestId, "", true, error);
    }
    
    public String getRequestId() { return requestId; }
    public String getPartialText() { return partialText; }
    public boolean isDone() { return done; }
    public Throwable getError() { return error; }
    public boolean hasError() { return error != null; }
    
    @Override
    public String toString() {
        return String.format("StreamEvent{requestId='%s', partialText='%s...', done=%b, hasError=%b}",
                requestId, partialText.substring(0, Math.min(20, partialText.length())), done, hasError());
    }
}
