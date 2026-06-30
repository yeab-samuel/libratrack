package com.libratrack.controller;
import com.libratrack.dto.request.CreateStaffRequest;
import com.libratrack.dto.request.ResetPasswordRequest;
import com.libratrack.dto.request.UpdateUserNameRequest;
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

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserDTO>> getUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(userService.getUsers(role, active, pageable));
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/users/by-university-id")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<UserDTO> getUserByUniversityId(@RequestParam String universityId) {
        return ResponseEntity.ok(userService.getUserByUniversityId(universityId));
    }

    @PatchMapping("/users/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> deactivateUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.deactivateUser(id));
    }

    @PatchMapping("/users/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> activateUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.activateUser(id));
    }

    @PatchMapping("/users/{id}/name")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> updateUserName(
            @PathVariable Long id, @Valid @RequestBody UpdateUserNameRequest req) {
        return ResponseEntity.ok(userService.updateFullName(id, req.fullName()));
    }

    /**
     * Admin resets any user's password directly.
     * BCrypt is one-way — there is no "view password" endpoint.
     * This is the correct pattern: admin sets a new known password,
     * tells the user in person, user logs in and changes it themselves.
     */
    @PatchMapping("/users/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest req) {
        userService.resetPassword(id, req.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> createStaff(@Valid @RequestBody CreateStaffRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.createStaff(req));
    }
}