package com.example.fraudapplication.triage;

import com.example.fraudapplication.triage.model.TriageAlert;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Loads the synthetic alert dataset. Reads from a filesystem path when available
 * (the canonical {@code demo/triage/alerts/alerts.json}) and otherwise falls back to
 * a bundled classpath copy, so the engine works both when run from the repo root and
 * when packaged as a jar.
 */
@Component
public class AlertDatasetLoader {

    private static final String CLASSPATH_FALLBACK = "triage/alerts.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<TriageAlert> load(String filesystemPath) {
        try {
            Path path = Path.of(filesystemPath);
            if (Files.exists(path)) {
                try (InputStream in = Files.newInputStream(path)) {
                    return read(in);
                }
            }
            ClassPathResource resource = new ClassPathResource(CLASSPATH_FALLBACK);
            try (InputStream in = resource.getInputStream()) {
                return read(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to load alert dataset from '" + filesystemPath
                            + "' or classpath '" + CLASSPATH_FALLBACK + "'", e);
        }
    }

    private List<TriageAlert> read(InputStream in) throws IOException {
        return objectMapper.readValue(in, new TypeReference<List<TriageAlert>>() {
        });
    }
}
