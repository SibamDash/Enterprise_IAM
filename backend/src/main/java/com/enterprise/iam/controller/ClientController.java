package com.enterprise.iam.controller;

import com.enterprise.iam.dto.client.ClientDto;
import com.enterprise.iam.dto.client.ClientSecretResponse;
import com.enterprise.iam.dto.client.CreateClientRequest;
import com.enterprise.iam.dto.client.UpdateClientRequest;
import com.enterprise.iam.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENT_READ')")
    public ResponseEntity<List<ClientDto>> getAllClients() {
        return ResponseEntity.ok(clientService.getAllClients());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENT_READ')")
    public ResponseEntity<ClientDto> getClientById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(clientService.getClientById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CLIENT_CREATE')")
    public ResponseEntity<ClientSecretResponse> createClient(@Valid @RequestBody CreateClientRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(clientService.createClient(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE')")
    public ResponseEntity<ClientDto> updateClient(@PathVariable String id, @Valid @RequestBody UpdateClientRequest request) {
        try {
            return ResponseEntity.ok(clientService.updateClient(id, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENT_DELETE')")
    public ResponseEntity<Void> deleteClient(@PathVariable String id) {
        try {
            clientService.deleteClient(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/rotate-secret")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE')")
    public ResponseEntity<ClientSecretResponse> rotateClientSecret(@PathVariable String id) {
        try {
            return ResponseEntity.ok(clientService.rotateClientSecret(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
