package com.libratrack.controller;

import com.libratrack.dto.response.RegistryDTO;
import com.libratrack.service.RegistryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/registry")
@RequiredArgsConstructor
public class RegistryController {

    private final RegistryService registryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Page<RegistryDTO>> getAll(
            @RequestParam(required = false) String role,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(registryService.getAll(role, pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RegistryDTO> addEntry(@Valid @RequestBody RegistryEntryRequest req) {
        RegistryDTO created = registryService.addEntry(
                req.universityId(), req.fullName(), req.role(), req.department());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeEntry(@PathVariable Long id) {
        registryService.removeEntry(id);
        return ResponseEntity.noContent().build();
    }
}