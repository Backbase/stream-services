package com.backbase.stream.worker.exception;

import com.backbase.stream.worker.model.StreamTask;
import reactor.core.publisher.Mono;

public class StreamTaskException extends RuntimeException {

    private StreamTask task;
    private String resolution;

    public StreamTaskException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public StreamTaskException(StreamTask task, Throwable throwable, String message) {
        super(message, throwable);
        this.task = task;
    }

    public StreamTaskException(StreamTask task, String message) {
        super(message);
        this.task = task;
    }


    /**
     * Generic exception thrown when StreamTask fails to execute.
     * @param streamTask The Stream Task
     * @param throwable The cause
     */
    public StreamTaskException(StreamTask streamTask, Throwable throwable) {
        super(throwable);
        this.task = streamTask;

    }

    public Mono<StreamTaskException> getMono() {
        return Mono.error(this);
    }

    public StreamTask getTask() {
        return task;
    }
}
