package com.aram3.domain;

public record Role(
        Long id,
        String code,
        String name,
        String description,
        boolean system
) {
}
