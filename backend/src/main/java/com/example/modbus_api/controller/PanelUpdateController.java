package com.example.modbus_api.controller;

import com.example.modbus_api.service.PanelUpdateService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/panel-updates")
@PreAuthorize("hasRole('ADMIN')")
public class PanelUpdateController {
    private final PanelUpdateService updates;
    public PanelUpdateController(PanelUpdateService updates) { this.updates = updates; }

    @GetMapping
    public ObjectNode status() throws IOException { return updates.status(); }

    @GetMapping("/history")
    public ObjectNode history() throws IOException { return updates.history(); }

    @PostMapping("/restore")
    public ObjectNode restore(@RequestBody Map<String, String> body) throws IOException {
        return updates.restore(body.get("version"));
    }

    @PostMapping
    public ObjectNode request(@RequestBody Map<String, String> body) throws IOException {
        return updates.request(body.get("version"));
    }
}
