package com.libratrack.dto.request;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
public record ExtendLoanRequest(
    @NotNull(message = "New due date is required")
    @Future(message = "New due date must be in the future")
    LocalDate newDueDate
) {}
