package com.libratrack.dto.request;

import jakarta.validation.constraints.*;

public record BookRatingRequest(
        @NotNull @Min(1) @Max(5) Integer stars,
        @Size(max = 500) String reviewText
) {}