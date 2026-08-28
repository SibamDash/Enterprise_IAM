package com.enterprise.iam.controller;

import com.enterprise.iam.dto.CreateOrganizationRequest;
import com.enterprise.iam.dto.OrganizationDto;
import com.enterprise.iam.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationDto createOrganization(@Valid @RequestBody CreateOrganizationRequest request) {
        return organizationService.createOrganization(request);
    }

    @GetMapping("/{id}")
    public OrganizationDto getOrganization(@PathVariable UUID id) {
        return organizationService.getOrganization(id);
    }

    @GetMapping
    public Page<OrganizationDto> listOrganizations(Pageable pageable) {
        return organizationService.listOrganizations(pageable);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateOrganization(@PathVariable UUID id) {
        organizationService.deactivateOrganization(id);
    }
}
