package org.example.amladapter.controller;

import org.example.amladapter.service.CheckResult;
import org.example.amladapter.service.ClientCheckService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientCheckController {
    private final ClientCheckService service;

    public ClientCheckController(ClientCheckService service) {
        this.service = service;
    }

    @PostMapping("/{id}/check")
    public ResponseEntity<?> checkClient(@PathVariable Long id){
        CheckResult result = service.checkClient(id);

        // TO DO

        return null;
    }
}
