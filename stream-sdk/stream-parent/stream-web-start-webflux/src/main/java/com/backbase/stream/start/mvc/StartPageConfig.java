package com.backbase.stream.start.mvc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.thymeleaf.spring5.ISpringWebFluxTemplateEngine;
import org.thymeleaf.spring5.SpringWebFluxTemplateEngine;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring5.view.reactive.ThymeleafReactiveViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ITemplateResolver;

@Configuration
@ComponentScan(basePackages = {"com.backbase.stream.start.mvc"})
@Slf4j
public class StartPageConfig {

    public StartPageConfig() {
        log.info("Adding Backbase Stream Start Page");
    }

    @Bean
    public VersionHolder getVersionHolder(ApplicationContext context) {
        return new VersionHolder(context);
    }

    @Bean
    public ViewResolver viewResolver(ApplicationContext applicationContext) {
        ThymeleafReactiveViewResolver resolver = new ThymeleafReactiveViewResolver();
        resolver.setTemplateEngine(templateEngine(applicationContext));
//        resolver.setCharacterEncoding("UTF-8");
        return resolver;
    }

    @Bean
    public ISpringWebFluxTemplateEngine templateEngine(ApplicationContext applicationContext) {
        SpringWebFluxTemplateEngine engine = new SpringWebFluxTemplateEngine();
        engine.setEnableSpringELCompiler(true);
        engine.setTemplateResolver(templateResolver(applicationContext));
        return engine;
    }

    private ITemplateResolver templateResolver(ApplicationContext applicationContext) {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setApplicationContext(applicationContext);
        resolver.setTemplateMode(TemplateMode.HTML);
        return resolver;
    }


}
