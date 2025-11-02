package com.gamereleasetracker.dto.api_dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Setter
@Getter
public class ApiGameDto {

    private String slug;

    private String name;

    private List<String> developers;

    private List<String> publishers;

    private Set<String> genres;

    private Set<String> platforms;

    private Set<String> tags;

    @JsonProperty("genres")
    private void unpackGenres(List<Map<String, Object>> genres) {
        this.genres = genres != null
            ? genres.stream()
                .map(genreMap -> (String) genreMap.get("name"))
                .collect(Collectors.toSet())
            : null;
    }

    @JsonProperty("platforms")
    private void unpackPlatforms(List<Map<String, Object>> platforms) {
        this.platforms = platforms != null
            ? platforms.stream()
                .map(platformMap -> (String) ((Map<String, Object>)platformMap.get("platform")).get("name"))
                .collect(Collectors.toSet())
            : null;
    }

    @JsonProperty("tags")
    private void unpacktags(List<Map<String, Object>> tags) {
        this.tags = tags != null
            ? tags.stream()
                .map(tagMap -> (String) tagMap.get("slug"))
                .collect(Collectors.toSet())
            : Collections.emptySet();
    }

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate released;

    @JsonProperty("background_image")
    private String backgroundImage;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updated;

    private String esrbRating;
    @JsonProperty("esrb_rating")
    private void unpackEsrbRating(Map<String, Object> esrbRating) {
        this.esrbRating = esrbRating != null ? (String) esrbRating.get("name") : null;
    }
}
