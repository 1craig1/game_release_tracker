package com.gamereleasetracker.dto.api_dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Setter
@Getter
public class ApiStoreDto {

    private int id;

    private String name;
}
