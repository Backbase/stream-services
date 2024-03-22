package com.backbase.stream.compositions.legalentity.core.mapper;

import com.backbase.stream.compositions.events.ingress.event.spec.v1.LegalEntityPullEvent;
import com.backbase.stream.compositions.legalentity.api.model.LegalEntityPullIngestionRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityPullRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityResponse;
import com.backbase.stream.compositions.legalentity.integration.client.model.LegalEntity;
import com.backbase.stream.compositions.legalentity.integration.client.model.PullLegalEntityRequest;
import com.backbase.stream.compositions.legalentity.integration.client.model.PullLegalEntityResponse;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

/**
 * This is a mapper for LegalEntity objects used in: - stream-models/legal-entity-model -
 * legal-entity-composition-api - legal-entity-integration-api - legal-entity-evens
 * <p>
 * All LegalEntity objects used in above modules have exactly same structures they are built from
 * the common /api folder.
 */

@Component
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface LegalEntityMapper {

    /**
     * Maps composition LegalEntity to stream LegalEntity model.
     *
     * @param legalEntity Integration legal entity
     * @return Stream legal entity
     */
    com.backbase.stream.legalentity.model.LegalEntity mapIntegrationToStream(
            LegalEntity legalEntity);

    /**
     * Maps composition LegalEntity to stream LegalEntity model.
     *
     * @param legalEntity Composition legal entity
     * @return Stream legal entity
     */
    com.backbase.stream.legalentity.model.LegalEntity mapCompostionToStream(
            com.backbase.stream.compositions.legalentity.api.model.LegalEntity legalEntity);

    /**
     * Maps stream LegalEntity to composition LegalEntity model.
     *
     * @param legalEntity Stream legal entity
     * @return Composition legal entity
     */
    com.backbase.stream.compositions.legalentity.api.model.LegalEntity mapStreamToComposition(
            com.backbase.stream.legalentity.model.LegalEntity legalEntity);

    /**
     * Maps event LegalEntity to stream LegalEntity model.
     *
     * @param legalEntity Event legal entity
     * @return Stream legal entity
     */
    @Mapping(target = "legalEntityType", source = "type")
    com.backbase.stream.legalentity.model.LegalEntity mapEventToStream(
            com.backbase.stream.compositions.events.ingress.event.spec.v1.LegalEntity legalEntity);

    /**
     * Maps stream LegalEntity to event LegalEntity model.
     *
     * @param legalEntity Stream legal entity
     * @return Event legal entity
     */
    com.backbase.stream.compositions.events.egress.event.spec.v1.LegalEntity mapStreamToEvent(
            com.backbase.stream.legalentity.model.LegalEntity legalEntity);

    /**
     * Maps composition input LegalEntityPullIngestionRequest to stream LegalEntityPullRequest
     *
     * @param legalEntityPullIngestionRequest
     * @return Stream Legal Entity Pull Request
     */
    LegalEntityPullRequest mapPullRequestCompositionToStream(
            LegalEntityPullIngestionRequest legalEntityPullIngestionRequest);

    /**
     * Maps composition input LegalEntityPullEvent to stream LegalEntityPullRequest
     *
     * @param legalEntityPullEvent
     * @return Stream Legal Entity Pull Request
     */
    LegalEntityPullRequest mapPullRequestEventToStream(LegalEntityPullEvent legalEntityPullEvent);

    /**
     * Maps stream input LegalEntityPullEvent to integration LegalEntityPullRequest
     *
     * @param legalEntityPullRequest
     * @return Integration Pull Legal Entity Request
     */
    PullLegalEntityRequest mapPullRequestStreamToIntegration(
            LegalEntityPullRequest legalEntityPullRequest);

    /**
     * Maps integration response to Stream response
     *
     * @param pullLegalEntityResponse
     * @return Stream Legal Entity Response
     */
    LegalEntityResponse mapResponseIntegrationToStream(
            PullLegalEntityResponse pullLegalEntityResponse);

}
