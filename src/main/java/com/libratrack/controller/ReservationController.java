package com.libratrack.controller;
import com.libratrack.dto.request.CreateReservationRequest;
import com.libratrack.dto.response.ReservationDTO;
import com.libratrack.enums.ReservationStatus;
import com.libratrack.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/reservations") @RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservationService;
    @PostMapping @PreAuthorize("hasAnyRole('STUDENT','FACULTY')") public ResponseEntity<ReservationDTO> createReservation(@Valid @RequestBody CreateReservationRequest req,Authentication auth){return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.createReservation(req,auth.getName()));}
    @DeleteMapping("/{id}") @PreAuthorize("hasAnyRole('STUDENT','FACULTY','ADMIN')") public ResponseEntity<Void> cancelReservation(@PathVariable Long id,Authentication auth){reservationService.cancelReservation(id,auth.getName());return ResponseEntity.noContent().build();}
    @GetMapping("/mine") @PreAuthorize("hasAnyRole('STUDENT','FACULTY')") public ResponseEntity<Page<ReservationDTO>> myReservations(Authentication auth,@PageableDefault(size=20) Pageable pageable){return ResponseEntity.ok(reservationService.getMyReservations(auth.getName(),pageable));}
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')") public ResponseEntity<Page<ReservationDTO>> allReservations(@RequestParam(required=false) Long bookId,@RequestParam(required=false) ReservationStatus status,@PageableDefault(size=20) Pageable pageable){return ResponseEntity.ok(reservationService.getAllReservations(bookId,status,pageable));}
}
