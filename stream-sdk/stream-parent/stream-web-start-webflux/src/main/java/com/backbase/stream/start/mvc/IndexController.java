package com.backbase.stream.start.mvc;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@EnableConfigurationProperties(StartPageConfigurationProperties.class)
@RequiredArgsConstructor
public class IndexController {

    private final StartPageConfigurationProperties properties;

    private final VersionHolder versionHolder;

    @GetMapping("/")
    public String index(Model model) {
        fillDefaultModelAttributes(model, properties, versionHolder);
        return "index.html";
    }

    public static void fillDefaultModelAttributes(Model model, StartPageConfigurationProperties properties, VersionHolder versionHolder) {
        model.addAttribute("title", properties.getTitle());
        model.addAttribute("subTitle", properties.getSubTitle());
        model.addAttribute("header", properties.getHeader());
        model.addAttribute("footer", properties.getFooter());
        model.addAttribute("description", properties.getDescription());
        model.addAttribute("version", versionHolder.getVersion());
        model.addAttribute("defaultAction", properties.getDefaultAction());
        model.addAttribute("navbarLinks", properties.getNavbarLinks());
    }

}
