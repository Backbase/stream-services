package com.backbase.stream.start.mvc;

import java.util.List;
import javax.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "backbase.stream.start")
@Data
@Validated
public class StartPageConfigurationProperties {

    @NotEmpty
    public String title;

    @NotEmpty
    public String header;

    @NotEmpty
    public String footer;

    @NotEmpty
    public String subTitle;

    @NotEmpty
    public String description;

    public Link defaultAction;


    public List<Link> navbarLinks;

    @Data
    public static class Link {
        private String name;
        private String href;
    }

}
