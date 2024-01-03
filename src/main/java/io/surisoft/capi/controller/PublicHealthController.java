package io.surisoft.capi.controller;

import io.surisoft.capi.service.ConsulNodeDiscovery;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class PublicHealthController {
    @GetMapping
    public ResponseEntity<String> amIHealthy() {
        if(ConsulNodeDiscovery.isConnectedToConsul()) {
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
