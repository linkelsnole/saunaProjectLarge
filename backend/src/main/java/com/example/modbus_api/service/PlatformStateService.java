package com.example.modbus_api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.io.InputStream;

@Service
public class PlatformStateService {
    private final PanelUpdateService updates;
    private final ObjectMapper mapper;
    private final String buildId;

    public PlatformStateService(PanelUpdateService updates, ObjectMapper mapper) throws IOException {
        this.updates = updates;
        this.mapper = mapper;
        ClassPathResource resource = new ClassPathResource("static/panel-build.json");
        if (resource.exists()) {
            try (InputStream input = resource.getInputStream()) {
                buildId = mapper.readTree(input).path("buildId").asText("");
            }
        } else buildId = ""; // Backend-only development has no bundled frontend.
    }

    public String buildId() { return buildId; }

    public boolean updating() throws IOException {
        String status = updates.status().path("status").asText();
        return "installing".equals(status) || "queued".equals(status);
    }

    public ObjectNode state() throws IOException {
        return mapper.createObjectNode().put("buildId", buildId).put("updating", updating());
    }
}
