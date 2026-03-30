package com.backbase.stream.investment.model;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.core.io.Resource;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"uuid", "image", "imageResource"})
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
