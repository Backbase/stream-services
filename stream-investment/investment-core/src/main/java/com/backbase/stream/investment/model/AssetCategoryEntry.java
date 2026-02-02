package com.backbase.stream.investment.model;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.core.io.Resource;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AssetCategoryEntry {

    private UUID uuid;
    private String name;
    private String code;
    private Integer order;
    private String type;
    private String excerpt;
    private String description;
    private String image;
    private Resource imageResource;

}
