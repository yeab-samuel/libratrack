package com.libratrack.controller;
import com.libratrack.dto.request.CreateStaffRequest;
import com.libratrack.dto.response.UserDTO;
import com.libratrack.enums.Role;
import com.libratrack.service.AuthService;
import com.libratrack.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/admin") @RequiredArgsConstructor
public class AdminController {
    private final UserService userService;
    private final AuthService authService;

    /** List all users with optional filters */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserDTO>> getUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(userService.getUsers(role, active, pageable));
    }

    /** Get a single user — lookup by ID */
    @GetMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /** Find a user by their university ID (for counter lookup) */
    @GetMapping("/users/by-university-id/{universityId}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<UserDTO> getUserByUniversityId(@PathVariable String universityId) {
        return ResponseEntity.ok(userService.getUserByUniversityId(universityId));
    }

    /** Deactivate a user account */
    @PatchMapping("/users/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> deactivateUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.deactivateUser(id));
    }

    /** Reactivate a user account */
    @PatchMapping("/users/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> activateUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.activateUser(id));
    }

    /** Create a LIBRARIAN or ADMIN staff account (no university ID needed) */
    @PostMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> createStaff(@Valid @RequestBody CreateStaffRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.createStaff(req));
    }
}
