package com.libratrack.dto.request;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record CreateLoanRequest(@NotNull Long memberId,@NotNull Long bookCopyId,@NotNull @Future LocalDate dueDate){}
