package com.aram3.common;

public record PageResult<T>(java.util.List<T> items, long total, int page, int size) {
    public static <T> PageResult<T> of(java.util.List<T> items, long total, int page, int size) {
        return new PageResult<>(items, total, page, size);
    }
}
