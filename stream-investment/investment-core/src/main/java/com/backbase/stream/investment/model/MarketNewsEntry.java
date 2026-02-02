package com.backbase.stream.investment.model;

import com.backbase.stream.investment.ModelAsset;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.core.io.Resource;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MarketNewsEntry {

    private String title;
    private String excerpt;
    private String body;
    @JsonProperty("video_url")
    private URI videoUrl;
    @JsonProperty("external_url")
    private URI externalUrl;
    private String thumbnail;
    private Resource thumbnailResource;
    private List<String> tags = new ArrayList<>();
    @JsonProperty("published_on")
    private OffsetDateTime publishedOn;
    private Integer order;
    private List<ModelAsset> assets = new ArrayList<>();

}
