package com.enterprise.iam.controller;

import com.enterprise.iam.dto.serviceaccount.CreateServiceAccountRequest;
import com.enterprise.iam.dto.serviceaccount.ServiceAccountDto;
import com.enterprise.iam.dto.serviceaccount.UpdateServiceAccountRequest;
import com.enterprise.iam.service.ServiceAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/service-accounts")
@RequiredArgsConstructor
public class ServiceAccountController {

    private final ServiceAccountService serviceAccountService;

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENT_READ')")
    public ResponseEntity<List<ServiceAccountDto>> getAllServiceAccounts() {
        return ResponseEntity.ok(serviceAccountService.getAllServiceAccounts());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENT_READ')")
    public ResponseEntity<ServiceAccountDto> getServiceAccountById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(serviceAccountService.getServiceAccountById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CLIENT_CREATE')")
    public ResponseEntity<ServiceAccountDto> createServiceAccount(@Valid @RequestBody CreateServiceAccountRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(serviceAccountService.createServiceAccount(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE')")
    public ResponseEntity<ServiceAccountDto> updateServiceAccount(@PathVariable String id, @Valid @RequestBody UpdateServiceAccountRequest request) {
        try {
            return ResponseEntity.ok(serviceAccountService.updateServiceAccount(id, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENT_DELETE')")
    public ResponseEntity<Void> deleteServiceAccount(@PathVariable String id) {
        try {
            serviceAccountService.deleteServiceAccount(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
