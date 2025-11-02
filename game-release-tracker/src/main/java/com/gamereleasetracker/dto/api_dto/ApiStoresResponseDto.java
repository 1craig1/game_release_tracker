package com.gamereleasetracker.dto.api_dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Setter
@Getter
public class ApiStoresResponseDto {
    private List<ApiStoreDto> results;
}
