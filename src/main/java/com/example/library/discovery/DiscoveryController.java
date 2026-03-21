package com.example.library.discovery;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/discovery")
class DiscoveryController {

    private final DiscoveryService discoveryService;

    DiscoveryController(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @GetMapping
    DiscoveryResponse getDiscovery() {
        return discoveryService.getDiscovery();
    }
}
