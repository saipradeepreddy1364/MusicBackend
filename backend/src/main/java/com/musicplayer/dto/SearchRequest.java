package com.musicplayer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SearchRequest {

    @NotBlank(message = "Query must not be blank")
    @Size(min = 1, max = 200, message = "Query must be between 1 and 200 characters")
    private String query;

    @Min(value = 1, message = "Page must be >= 1")
    private int page = 1;

    @Min(value = 1, message = "Limit must be >= 1")
    private int limit = 20;
}
