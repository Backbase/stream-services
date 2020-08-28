package com.backbase.stream.cursor.configuration;

import com.backbase.stream.TransactionService;
import com.backbase.stream.cursor.CursorStreamService;
import com.backbase.stream.cursor.events.ArrangementListener;
import com.backbase.stream.cursor.events.AuditLoginEventListener;
import com.backbase.stream.cursor.events.LoginEventListener;
import com.backbase.stream.cursor.events.PaymentListener;
import com.backbase.stream.service.EntitlementsService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cursor Source Configuration.
 */
@Configuration
@EnableConfigurationProperties(CursorServiceConfigurationProperties.class)
public class CursorServiceConfiguration {

    @Bean
    public LoginEventListener loginEventListener(EntitlementsService entitlementsService,
                                                 TransactionService transactionService,
                                                 CursorServiceConfigurationProperties properties) {
        return new LoginEventListener(entitlementsService, transactionService, properties);
    }

    @Bean
    public AuditLoginEventListener auditLoginEventListener(EntitlementsService entitlementsService,
                                                           TransactionService transactionService,
                                                           CursorServiceConfigurationProperties properties) {
        return new AuditLoginEventListener(entitlementsService, transactionService, properties);
    }

    @Bean
    public ArrangementListener arrangementListener() {
        return new ArrangementListener();
    }

    @Bean
    public PaymentListener paymentListener() {
        return new PaymentListener();
    }

    @Bean
    public CursorStreamService cursorStreamService(LoginEventListener loginEventListener,
                                                   ArrangementListener arrangementAddedListener,
                                                   AuditLoginEventListener auditLoginEventListener) {
        return new CursorStreamService(loginEventListener, auditLoginEventListener, arrangementAddedListener, paymentListener());
    }

}
