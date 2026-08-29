package com.enterprise.iam.controller;

import com.enterprise.iam.dto.CreateGroupRequest;
import com.enterprise.iam.dto.GroupDto;
import com.enterprise.iam.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PreAuthorize("hasAuthority('GROUP_CREATE')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupDto createGroup(@Valid @RequestBody CreateGroupRequest request) {
        return groupService.createGroup(request);
    }

    @PreAuthorize("hasAuthority('GROUP_READ')")
    @GetMapping("/{id}")
    public GroupDto getGroup(@PathVariable UUID id) {
        return groupService.getGroup(id);
    }

    @PreAuthorize("hasAuthority('GROUP_READ')")
    @GetMapping
    public Page<GroupDto> listGroups(Pageable pageable) {
        return groupService.listGroups(pageable);
    }

    @PreAuthorize("hasAuthority('GROUP_UPDATE')")
    @PutMapping("/{id}/roles")
    public GroupDto assignRoles(@PathVariable UUID id, @Valid @RequestBody com.enterprise.iam.dto.AssignRolesRequest request) {
        return groupService.assignRoles(id, request.getRoleIds());
    }

    @PreAuthorize("hasAuthority('GROUP_DELETE')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(@PathVariable UUID id) {
        groupService.deleteGroup(id);
    }
}
