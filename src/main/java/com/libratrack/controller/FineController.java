package com.libratrack.controller;
import com.libratrack.dto.request.WaiveRequest;
import com.libratrack.dto.response.FineDTO;
import com.libratrack.enums.FineStatus;
import com.libratrack.service.FineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/fines") @RequiredArgsConstructor
public class FineController {
    private final FineService fineService;
    @GetMapping("/mine") @PreAuthorize("hasAnyRole('STUDENT','FACULTY')") public ResponseEntity<Page<FineDTO>> myFines(Authentication auth,@PageableDefault(size=20) Pageable pageable){return ResponseEntity.ok(fineService.getMyFines(auth.getName(),pageable));}
    @GetMapping("/{id}") public ResponseEntity<FineDTO> getFineById(@PathVariable Long id,Authentication auth){return ResponseEntity.ok(fineService.getFineById(id,auth.getName()));}
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')") public ResponseEntity<Page<FineDTO>> allFines(@RequestParam(required=false) Long memberId,@RequestParam(required=false) FineStatus status,@PageableDefault(size=20) Pageable pageable){return ResponseEntity.ok(fineService.getAllFines(memberId,status,pageable));}
    @PatchMapping("/{id}/pay") @PreAuthorize("hasRole('LIBRARIAN')") public ResponseEntity<FineDTO> markPaid(@PathVariable Long id,Authentication auth){return ResponseEntity.ok(fineService.markAsPaid(id,auth.getName()));}
    @PatchMapping("/{id}/waive") @PreAuthorize("hasRole('ADMIN')") public ResponseEntity<FineDTO> waive(@PathVariable Long id,@Valid @RequestBody WaiveRequest req,Authentication auth){return ResponseEntity.ok(fineService.waiveFine(id,req,auth.getName()));}
}
