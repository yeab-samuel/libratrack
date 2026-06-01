package com.libratrack.controller;
import com.libratrack.entity.UniversityRegistry;
import com.libratrack.enums.Role;
import com.libratrack.exception.DuplicateResourceException;
import com.libratrack.repository.UniversityRegistryRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/registry")
@RequiredArgsConstructor
public class RegistryController {
    private final UniversityRegistryRepository registryRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Page<UniversityRegistry>> getAll(
            @RequestParam(required = false) String role,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(registryRepository.findAll(pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UniversityRegistry> addEntry(
            @Valid @RequestBody RegistryEntryRequest req) {
        if (registryRepository.existsByUniversityId(req.universityId()))
            throw new DuplicateResourceException(
                    "University ID already in registry: " + req.universityId());

        UniversityRegistry entry = UniversityRegistry.builder()
                .universityId(req.universityId())
                .fullName(req.fullName())
                .role(req.role())
                .department(req.department())
                .active(true)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registryRepository.save(entry));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeEntry(@PathVariable Long id) {
        registryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}