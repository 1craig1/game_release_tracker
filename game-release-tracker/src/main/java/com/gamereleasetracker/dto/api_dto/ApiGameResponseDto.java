package com.gamereleasetracker.dto.api_dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Setter
@Getter
public class ApiGameResponseDto {

    private int count;

    private String next;

    private String previous;

    private List<ApiGameDto> results;
}
