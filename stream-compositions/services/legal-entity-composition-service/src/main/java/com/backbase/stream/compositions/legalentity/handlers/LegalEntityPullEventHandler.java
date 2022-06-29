package com.backbase.stream.compositions.legalentity.handlers;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.handler.EventHandler;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.stream.compositions.events.egress.event.spec.v1.LegalEntityCompletedEvent;
import com.backbase.stream.compositions.events.egress.event.spec.v1.LegalEntityFailedEvent;
import com.backbase.stream.compositions.events.ingress.event.spec.v1.LegalEntityPullEvent;
import com.backbase.stream.compositions.legalentity.core.config.LegalEntityConfigurationProperties;
import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapper;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityPullRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityResponse;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityIngestionService;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@AllArgsConstructor
@EnableConfigurationProperties(LegalEntityConfigurationProperties.class)
public class LegalEntityPullEventHandler implements EventHandler<LegalEntityPullEvent> {

  private final LegalEntityConfigurationProperties configProperties;
  private final LegalEntityIngestionService legalEntityIngestionService;
  private final LegalEntityMapper mapper;
  private final EventBus eventBus;

  /**
   * Handles LegalEntityIngestPullEvent.
   *
   * @param envelopedEvent EnvelopedEvent<LegalEntityPullEvent>
   */
  @Override
  public void handle(EnvelopedEvent<LegalEntityPullEvent> envelopedEvent) {
    legalEntityIngestionService.ingestPull(buildRequest(envelopedEvent.getEvent()))
        .doOnSuccess(this::handleResponse)
        .onErrorResume(this::handleError)
            .subscribe();
  }

  /**
   * Builds ingestion request for downstream service.
   *
   * @param event LegalEntityIngestPullEvent
   * @return LegalEntityIngestPullRequest
   */
  private LegalEntityPullRequest buildRequest(LegalEntityPullEvent event) {
    return mapper.mapPullRequestEventToStream(event);
  }

  /**
   * Handles response from ingestion service.
   *
   * @param response LegalEntityIngestResponse
   */
  private void handleResponse(LegalEntityResponse response) {
    if (Boolean.TRUE.equals(configProperties.getEvents().getEnableCompleted())) {
      sendCompletedEvent(response);
    }
  }

  private void sendCompletedEvent(LegalEntityResponse response) {
    LegalEntityCompletedEvent event = new LegalEntityCompletedEvent()
        .withLegalEntity(mapper.mapStreamToEvent(response.getLegalEntity()));

    EnvelopedEvent<LegalEntityCompletedEvent> envelopedEvent = new EnvelopedEvent<>();
    envelopedEvent.setEvent(event);
    eventBus.emitEvent(envelopedEvent);
  }

  /**
   * Handles error from ingestion service.
   *
   * @param ex Throwable
   */
  private Mono<LegalEntityResponse> handleError(Throwable ex) {
    if (Boolean.TRUE.equals(configProperties.getEvents().getEnableFailed())) {
      LegalEntityFailedEvent event = new LegalEntityFailedEvent()
              .withEventId(UUID.randomUUID().toString())
              .withMessage(ex.getMessage());

      EnvelopedEvent<LegalEntityFailedEvent> envelopedEvent = new EnvelopedEvent<>();
      envelopedEvent.setEvent(event);
      eventBus.emitEvent(envelopedEvent);
    }

    return Mono.empty();
  }
}
