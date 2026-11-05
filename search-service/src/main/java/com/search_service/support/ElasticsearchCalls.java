package com.search_service.support;

import java.io.IOException;
import java.io.UncheckedIOException;

public final class ElasticsearchCalls {

    private ElasticsearchCalls() {
    }

    public static <T> T call(IoSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @FunctionalInterface
    public interface IoSupplier<T> {
        T get() throws IOException;
    }
}
