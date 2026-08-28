package com.enterprise.iam.controller;

import com.enterprise.iam.dto.CreateRoleRequest;
import com.enterprise.iam.dto.RoleDto;
import com.enterprise.iam.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoleDto createRole(@Valid @RequestBody CreateRoleRequest request) {
        return roleService.createRole(request);
    }

    @GetMapping("/{id}")
    public RoleDto getRole(@PathVariable UUID id) {
        return roleService.getRole(id);
    }

    @GetMapping
    public Page<RoleDto> listRoles(Pageable pageable) {
        return roleService.listRoles(pageable);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRole(@PathVariable UUID id) {
        roleService.deleteRole(id);
    }
}
