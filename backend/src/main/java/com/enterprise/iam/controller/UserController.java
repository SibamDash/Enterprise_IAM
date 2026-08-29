package com.enterprise.iam.controller;

import com.enterprise.iam.dto.CreateUserRequest;
import com.enterprise.iam.dto.UserDto;
import com.enterprise.iam.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PreAuthorize("hasAuthority('USER_CREATE')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    @PreAuthorize("hasAuthority('USER_READ')")
    @GetMapping("/{id}")
    public UserDto getUser(@PathVariable UUID id) {
        return userService.getUser(id);
    }

    @PreAuthorize("hasAuthority('USER_READ')")
    @GetMapping
    public Page<UserDto> listUsers(Pageable pageable) {
        return userService.listUsers(pageable);
    }

    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @PutMapping("/{id}/roles")
    public UserDto assignRoles(@PathVariable UUID id, @Valid @RequestBody com.enterprise.iam.dto.AssignRolesRequest request) {
        return userService.assignRoles(id, request.getRoleIds());
    }

    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @PutMapping("/{id}/groups")
    public UserDto assignGroups(@PathVariable UUID id, @Valid @RequestBody com.enterprise.iam.dto.AssignGroupsRequest request) {
        return userService.assignGroups(id, request.getGroupIds());
    }

    @PreAuthorize("hasAuthority('USER_DELETE')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateUser(@PathVariable UUID id) {
        userService.deactivateUser(id);
    }
}
