package com.libratrack.dto.request;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

/** Used by STUDENT or FACULTY to borrow a book directly (self-service). */
public record BorrowRequest(
    @NotNull Long bookCopyId,
    @NotNull @Future LocalDate dueDate
) {}
