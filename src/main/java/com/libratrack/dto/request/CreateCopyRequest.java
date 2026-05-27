package com.libratrack.dto.request;
import com.libratrack.enums.CopyCondition;
import jakarta.validation.constraints.*;
public record CreateCopyRequest(@NotBlank @Size(max=30) String copyNumber,@NotNull CopyCondition condition){}
