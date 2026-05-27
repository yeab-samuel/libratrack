package com.libratrack.dto.request;
import jakarta.validation.constraints.NotBlank;
public record WaiveRequest(@NotBlank String reason){}
