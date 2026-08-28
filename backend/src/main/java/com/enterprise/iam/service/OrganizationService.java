package com.enterprise.iam.service;

import com.enterprise.iam.domain.Organization;
import com.enterprise.iam.dto.CreateOrganizationRequest;
import com.enterprise.iam.dto.OrganizationDto;
import com.enterprise.iam.repository.OrganizationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    @Transactional
    public OrganizationDto createOrganization(CreateOrganizationRequest request) {
        Organization org = new Organization();
        org.setName(request.getName());
        
        org = organizationRepository.save(org);
        return mapToDto(org);
    }

    @Transactional(readOnly = true)
    public OrganizationDto getOrganization(UUID id) {
        return organizationRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found"));
    }

    @Transactional(readOnly = true)
    public Page<OrganizationDto> listOrganizations(Pageable pageable) {
        return organizationRepository.findAll(pageable)
                .map(this::mapToDto);
    }

    @Transactional
    public void deactivateOrganization(UUID id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found"));
        org.setStatus("INACTIVE");
        organizationRepository.save(org);
    }

    private OrganizationDto mapToDto(Organization org) {
        OrganizationDto dto = new OrganizationDto();
        dto.setId(org.getId());
        dto.setName(org.getName());
        dto.setStatus(org.getStatus());
        dto.setCreatedAt(org.getCreatedAt());
        dto.setUpdatedAt(org.getUpdatedAt());
        return dto;
    }
}
