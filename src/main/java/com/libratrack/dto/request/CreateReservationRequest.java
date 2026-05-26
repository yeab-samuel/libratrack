package com.libratrack.dto.request;
import jakarta.validation.constraints.NotNull;
public record CreateReservationRequest(@NotNull Long bookId){}
