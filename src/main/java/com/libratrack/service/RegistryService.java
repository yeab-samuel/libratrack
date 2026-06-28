package com.libratrack.service;

import com.libratrack.dto.response.RegistryDTO;
import com.libratrack.entity.UniversityRegistry;
import com.libratrack.enums.Role;
import com.libratrack.exception.DuplicateResourceException;
import com.libratrack.exception.ResourceNotFoundException;
import com.libratrack.repository.UniversityRegistryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistryService {

    private final UniversityRegistryRepository registryRepository;

    @Transactional(readOnly = true)
    public Page<RegistryDTO> getAll(String role, Pageable pageable) {
        return registryRepository.findAll(pageable).map(RegistryDTO::from);
    }

    @Transactional
    public RegistryDTO addEntry(String universityId, String fullName, Role role, String department) {
        if (registryRepository.existsByUniversityId(universityId))
            throw new DuplicateResourceException("University ID already in registry: " + universityId);
        UniversityRegistry entry = UniversityRegistry.builder()
                .universityId(universityId)
                .fullName(fullName)
                .role(role)
                .department(department)
                .active(true)
                .build();
        RegistryDTO saved = RegistryDTO.from(registryRepository.save(entry));
        log.info("Registry entry added: {} {} ({})", role, universityId, fullName);
        return saved;
    }

    @Transactional
    public void removeEntry(Long id) {
        if (!registryRepository.existsById(id))
            throw new ResourceNotFoundException("Registry entry not found: " + id);
        registryRepository.deleteById(id);
        log.info("Registry entry removed: id={}", id);
    }
}