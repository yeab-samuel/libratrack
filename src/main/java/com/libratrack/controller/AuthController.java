package com.libratrack.controller;
import com.libratrack.dto.request.*;
import com.libratrack.dto.response.*;
import com.libratrack.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    @PostMapping("/register") public ResponseEntity<UserDTO> register(@Valid @RequestBody RegisterRequest req){return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));}
    @PostMapping("/login") public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req){return ResponseEntity.ok(authService.login(req));}
    @PostMapping("/logout") public ResponseEntity<Void> logout(@RequestHeader("Authorization") String bearer){authService.logout(bearer);return ResponseEntity.noContent().build();}
}
