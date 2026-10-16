package com.example.modbus_api.controller;

import com.example.modbus_api.service.PlatformStateService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;

@RestController
public class PlatformStateController {
    private final PlatformStateService platform;
    public PlatformStateController(PlatformStateService platform) { this.platform = platform; }

    @GetMapping("/api/v1/platform-state")
    public ResponseEntity<ObjectNode> state() throws IOException {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(platform.state());
    }
}
