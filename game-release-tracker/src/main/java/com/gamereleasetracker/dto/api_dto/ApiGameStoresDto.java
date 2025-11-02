package com.gamereleasetracker.dto.api_dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Setter
@Getter
public class ApiGameStoresDto {

    @JsonProperty("store_id")
    private int storeId;

    private String url;
}
