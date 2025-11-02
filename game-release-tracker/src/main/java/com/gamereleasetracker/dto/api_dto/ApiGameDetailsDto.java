package com.gamereleasetracker.dto.api_dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@Setter
@Getter
public class ApiGameDetailsDto {

    @JsonProperty("description_raw")
    private String description;

    private String developer;

    private String publisher;

    @JsonProperty("developers")
    private void unpackDevelopers(List<Map<String, Object>> developers) {
        this.developer = developers != null && developers.size() > 0
            ?  (String) developers.get(0).get("name")
            : null;
    }

    @JsonProperty("publishers")
    private void unpackPublisher(List<Map<String, Object>> publisher) {
        this.publisher = publisher != null && publisher.size() > 0
            ?  (String) publisher.get(0).get("name")
            : null;
    }
}
