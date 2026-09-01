package com.devsphere.user.idempotency;

public class EventProcessingResult<T> {

    private final boolean processed;
    private final boolean duplicate;
    private final T data;

    private EventProcessingResult(boolean processed, boolean duplicate, T data) {
        this.processed = processed;
        this.duplicate = duplicate;
        this.data = data;
    }

    public static <T> EventProcessingResult<T> success(T data) {
        return new EventProcessingResult<>(true, false, data);
    }

    public static <T> EventProcessingResult<T> duplicate() {
        return new EventProcessingResult<>(false, true, null);
    }

    public boolean isProcessed() {
        return processed;
    }

    public boolean isDuplicate() {
        return duplicate;
    }

    public T getData() {
        return data;
    }
}
