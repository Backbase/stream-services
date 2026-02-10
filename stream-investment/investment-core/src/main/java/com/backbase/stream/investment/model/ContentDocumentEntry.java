package com.backbase.stream.investment.model;

import com.backbase.stream.investment.ModelAsset;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.core.io.Resource;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentDocumentEntry {

    private String name;
    private String description;
    private List<String> tags;
    private List<ModelAsset> assets;
    @JsonProperty("extra_data")
    private Map<String, Object> extraData;
    private String path;
    private Resource resourceInPath;

}
