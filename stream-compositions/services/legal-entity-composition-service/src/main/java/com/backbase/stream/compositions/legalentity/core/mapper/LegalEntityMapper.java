package com.backbase.stream.compositions.legalentity.core.mapper;

import com.backbase.stream.compositions.legalentity.integration.client.model.LegalEntity;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

/**
 * This is a mapper for LegalEntity objects used in:
 * - stream-models/legal-entity-model
 * - legal-entity-composition-api
 * - legal-entity-integration-api
 * - legal-entity-evens
 * <p>
 * All LegalEntity objects used in above modules have exactly same structures they are built
 * from the common /api folder.
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
     * Maps integration LegalEntity to composition LegalEntity model.
     *
     * @param legalEntity Integration legal entity
     * @return Composition legal entity
     */
    com.backbase.stream.compositions.legalentity.api.model.LegalEntity mapIntegrationToCompostion(
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
    com.backbase.stream.legalentity.model.LegalEntity mapEventToStream(
            com.backbase.stream.compositions.events.ingress.event.spec.v1.LegalEntity legalEntity);

    /**
     * Maps steam LegalEntity to event LegalEntity model.
     *
     * @param legalEntity Stream legal entity
     * @return Event legal entity
     */
    com.backbase.stream.compositions.events.egress.event.spec.v1.LegalEntity mapStreamToEvent(
            com.backbase.stream.legalentity.model.LegalEntity legalEntity);
}
