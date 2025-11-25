package com.example.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Singleton;

/**
 * Configures Jackson ObjectMapper for REST responses.
 * Enables pretty-printing (indented output) for better readability.
 */
@Singleton
public class JacksonConfig implements ObjectMapperCustomizer {
    @Override
    public void customize(ObjectMapper mapper) {
        // Enable pretty printing (indented JSON output)
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
}