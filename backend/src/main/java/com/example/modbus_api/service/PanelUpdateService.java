package com.example.modbus_api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;

/** File mailbox for the external updater. This application never executes deployment commands. */
@Service
public class PanelUpdateService {
    private final Path directory;
    private final ObjectMapper mapper;
    private final String channel;

    public PanelUpdateService(@Value("${panel.updates.directory:${user.home}/.local/state/sauna-updater-large}") String directory,
                              ObjectMapper mapper,
                              @Value("${panel.updates.channel:production}") String channel) {
        this.directory = Paths.get(directory).toAbsolutePath().normalize();
        this.mapper = mapper;
        if (!"production".equals(channel) && !"lab".equals(channel)) throw new IllegalArgumentException("Invalid update channel");
        this.channel = channel;
    }

    private JsonNode read(String name) throws IOException {
        Path path = directory.resolve(name);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.size(path) > 32768) {
            return mapper.createObjectNode();
        }
        JsonNode value = mapper.readTree(Files.readAllBytes(path));
        return value != null && value.isObject() ? value : mapper.createObjectNode();
    }

    private boolean enabled() {
        return !Files.isSymbolicLink(directory) && Files.isRegularFile(directory.resolve("ui-enabled"), LinkOption.NOFOLLOW_LINKS);
    }

    public synchronized ObjectNode status() throws IOException {
        ObjectNode result = mapper.createObjectNode();
        result.put("enabled", enabled());
        if (!enabled()) return result;
        JsonNode state = read("status.json");
        String status = state.path("status").asText("waiting");
        String tag = state.path("tag").asText("");
        long checkedAt = Math.max(state.path("checked_at").asLong(0), read("heartbeat.json").path("checked_at").asLong(0));
        boolean fresh = checkedAt > Instant.now().getEpochSecond() - 180;
        JsonNode request = read("request.json");
        if ("available".equals(status) && tag.equals(request.path("tag").asText()) && !tag.isEmpty()) status = "queued";
        if (request.path("restore").asBoolean() && !"installing".equals(status)) {
            status = "queued";
            tag = request.path("tag").asText();
        }
        result.put("installedVersion", read("history.json").path("current").asText(""));
        result.put("status", status);
        result.put("version", tag);
        result.put("commit", state.path("commit").asText(""));
        result.put("ready", fresh);
        result.put("checkedAt", checkedAt);
        return result;
    }

    public synchronized ObjectNode request(String version) throws IOException {
        if (version == null || !version.matches("large-" + channel + "-[1-9][0-9]*-[1-9][0-9]*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректная версия");
        }
        ObjectNode current = status();
        if (!current.path("enabled").asBoolean() || !current.path("ready").asBoolean()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Загрузчик обновлений недоступен");
        }
        if (!version.equals(current.path("version").asText()) ||
                !("available".equals(current.path("status").asText()) || "queued".equals(current.path("status").asText()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Версия уже изменилась. Обновите сведения");
        }
        return queue(version, false);
    }

    public synchronized ObjectNode history() throws IOException {
        ObjectNode result = mapper.createObjectNode().put("enabled", enabled());
        com.fasterxml.jackson.databind.node.ArrayNode versions = result.putArray("versions");
        if (!enabled()) return result;
        JsonNode saved = read("history.json");
        String current = saved.path("current").asText();
        for (JsonNode entry : saved.path("versions")) {
            if (versions.size() == 3) break;
            ObjectNode item = versions.addObject();
            for (String field : new String[]{"version", "commit", "description", "installedAt", "databaseSavedAt"})
                if (entry.has(field)) item.set(field, entry.get(field));
            item.put("current", current.equals(entry.path("version").asText()));
            item.put("restorable", entry.path("restorable").asBoolean() && !item.path("current").asBoolean());
        }
        return result;
    }

    public synchronized ObjectNode restore(String version) throws IOException {
        ObjectNode current = status();
        if (!current.path("enabled").asBoolean() || !current.path("ready").asBoolean())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Загрузчик недоступен");
        if ("installing".equals(current.path("status").asText()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Установка уже выполняется");
        boolean found = false;
        for (JsonNode entry : history().path("versions"))
            if (entry.path("version").asText().equals(version) && entry.path("restorable").asBoolean()) found = true;
        if (!found) throw new ResponseStatusException(HttpStatus.CONFLICT, "Версия недоступна для восстановления");
        return queue(version, true);
    }

    private ObjectNode queue(String version, boolean restore) throws IOException {
        JsonNode existing = read("request.json");
        if (existing.has("tag") && (!version.equals(existing.path("tag").asText()) || restore != existing.path("restore").asBoolean()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Другая операция уже ожидает выполнения");
        Path temporary = Files.createTempFile(directory, ".request-", ".json",
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        try {
            ObjectNode request = mapper.createObjectNode().put("tag", version).put("restore", restore);
            Files.write(temporary, mapper.writeValueAsBytes(request), StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(temporary, directory.resolve("request.json"), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
        return status();
    }
}
