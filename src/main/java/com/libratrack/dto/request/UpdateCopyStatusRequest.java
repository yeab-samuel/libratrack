package com.libratrack.dto.request;
import com.libratrack.enums.*;
import jakarta.validation.constraints.NotNull;
public record UpdateCopyStatusRequest(@NotNull CopyStatus status,CopyCondition condition){}
