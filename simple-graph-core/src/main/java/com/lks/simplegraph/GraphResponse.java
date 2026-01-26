package com.lks.simplegraph;

import java.util.Optional;

/**
 * 图执行的响应结果包装类
 */
public class GraphResponse<T> {

    private final T output;
    private final String status;

    private final Throwable error;

    private GraphResponse(T output, String status, Throwable error) {
        this.output = output;
        this.status = status;
        this.error = error;
    }

    public static <T> GraphResponse<T> of(T output) {
        return new GraphResponse<>(output, "running", null);
    }

    public static <T> GraphResponse<T> done(T output) {
        return new GraphResponse<>(output, "done", null);
    }

    public static <T> GraphResponse<T> error(Throwable error) {
        return new GraphResponse<>(null, "error", error);
    }

    public T getOutput() {
        return output;
    }

    public String getStatus() {
        return status;
    }

    public Optional<T> resultValue() {
        return Optional.ofNullable(output);
    }

    public Optional<Throwable> getError() {
        return Optional.ofNullable(error);
    }

    public boolean isDone() {
        return "done".equals(status);
    }

    public boolean isError() {
        return "error".equals(status);
    }
}