package com.backbase.stream.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties("backbase.stream.user.management")
public class UserManagementProperties {

    private boolean updateIdentity = true;

}
