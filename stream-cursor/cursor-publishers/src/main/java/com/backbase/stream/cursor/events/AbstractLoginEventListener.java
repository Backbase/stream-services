package com.backbase.stream.cursor.events;

import com.backbase.dbs.accounts.presentation.service.model.ArrangementItem;
import com.backbase.stream.TransactionService;
import com.backbase.stream.cursor.configuration.CursorServiceConfigurationProperties;
import com.backbase.stream.cursor.model.IngestionCursor;
import com.backbase.stream.exceptions.UserNotFoundException;
import com.backbase.stream.legalentity.model.AssignedPermission;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.legalentity.model.User;
import com.backbase.stream.service.EntitlementsService;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

/**
 * From a Login Event, Get Assigned Permissions and retrieve last transaction.
 */
@Slf4j
public class AbstractLoginEventListener {

    public static final String RESOURCE_NAME = "Transactions";
    public static final String PRIVILEGE = "view";

    private static final String LATEST_TRANSACTION = "latestTransaction";

    private final DirectProcessor<IngestionCursor> processor = DirectProcessor.create();
    private final FluxSink<IngestionCursor> sink = processor.sink();

    private final EntitlementsService entitlementsService;
    private final TransactionService transactionService;
    private final CursorServiceConfigurationProperties cursorServiceConfigurationProperties;

    public AbstractLoginEventListener(EntitlementsService entitlementsService,
                                      TransactionService transactionService,
                                      CursorServiceConfigurationProperties cursorServiceConfigurationProperties) {
        this.entitlementsService = entitlementsService;
        this.transactionService = transactionService;
        this.cursorServiceConfigurationProperties = cursorServiceConfigurationProperties;
    }


    /**
     * Publisher emitting Ingestion Cursors.
     *
     * @return Publisher of Ingestion Cursors
     */
    public Publisher<IngestionCursor> getLoginEventProcessor() {
        return processor;
    }

    public void publishIngestionCursorsFor(Object loginEvent, String username) {
        entitlementsService.getLegalEntityForUserName(username)
            .doOnError(UserNotFoundException.class, e -> log.info("User: {} not found in DBS", username))
            .onErrorResume(UserNotFoundException.class, e -> Mono.empty())
            .doOnNext(userAndLegalEntity -> {
                log.info("Retrieved legalEntity: {} with user: {}", userAndLegalEntity.getT2().getInternalId(), username);
                // Send Login Event
                User user = userAndLegalEntity.getT1();
                LegalEntity legalEntity = userAndLegalEntity.getT2();
                IngestionCursor loginIngestionCursor = createLoginIngestionCursor(loginEvent, user, legalEntity);
                log.info("Publishing Login Event for user: {} with legal entity : {}", user.getExternalId(), legalEntity.getExternalId());
                sink.next(loginIngestionCursor);

                if (cursorServiceConfigurationProperties.isPublishEntitledArrangements()) {
                    entitlementsService.getAssignedPermissionsForUser(RESOURCE_NAME, RESOURCE_NAME, PRIVILEGE, user)
                        .flatMap(assignedPermission -> createIngestionCursor(user, legalEntity, loginEvent, assignedPermission))
                        .subscribe(sink::next);
                }
            })
            .subscribe();

    }

    private Flux<IngestionCursor> createIngestionCursor(User user, LegalEntity legalEntity, Object loginEvent, AssignedPermission assignedPermission) {
        List<IngestionCursor> ingestionCursors = new ArrayList<>();
        ProductGroup.ProductGroupTypeEnum arrangements1 = ProductGroup.ProductGroupTypeEnum.ARRANGEMENTS;
        Object arrangements = assignedPermission.getPermittedObjects().get(arrangements1.name());
        if (arrangements instanceof List && (((List) arrangements).get(0) instanceof ArrangementItem)) {
            List<ArrangementItem> products = (List<ArrangementItem>) arrangements;

            for (ArrangementItem product : products) {

                IngestionCursor ingestionCursor = createLoginIngestionCursor(loginEvent, user, legalEntity);

                ingestionCursor.setArrangementId(product.getId());
                ingestionCursor.setExternalArrangementId(product.getExternalArrangementId());
                // Make this configurable?
                ingestionCursor.setCursorState(IngestionCursor.CursorStateEnum.NOT_STARTED);
                ingestionCursor.setCursorSource(IngestionCursor.CursorSourceEnum.LOGIN_EVENT);
                ingestionCursor.getAdditionalProperties().put("product", product);
                ingestionCursors.add(ingestionCursor);
            }
        }

        return Flux.fromIterable(ingestionCursors);

    }

    private IngestionCursor createLoginIngestionCursor(Object loginEvent, User user, LegalEntity legalEntity) {
        IngestionCursor ingestionCursor = new IngestionCursor();
        ingestionCursor.setId(UUID.randomUUID());
        ingestionCursor.setCursorState(IngestionCursor.CursorStateEnum.NOT_STARTED);
        ingestionCursor.setCursorSource(IngestionCursor.CursorSourceEnum.LOGIN_EVENT);
        ingestionCursor.setExternalUserId(user.getExternalId());
        ingestionCursor.setInternalUserId(user.getInternalId());
        ingestionCursor.setCursorCreatedAt(OffsetDateTime.now());
        ingestionCursor.setInternalLegalEntityId(legalEntity.getInternalId());
        ingestionCursor.setExternalLegalEntityId(legalEntity.getExternalId());
        ingestionCursor.setAdditionalProperties(new LinkedHashMap<>());
        ingestionCursor.getAdditionalProperties().put("loginEvent", loginEvent);
        ingestionCursor.getAdditionalProperties().put("legalEntity", legalEntity);
        ingestionCursor.getAdditionalProperties().put("user", user);
        return ingestionCursor;
    }


}
