package com.backbase.stream.compositions.legalentity.core.mapper;

import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

/**
 * This is a mapper for LegalEntity objects used in:
 * - stream-models/legal-entity-model
 * - legal-entity-composition-api
 * - legal-entity-integration-api
 * - legal-entity-evens
 *
 * All LegalEntity objects used in above modules have exactly same structureas they are built
 * from the common /api folder.
 */
@Mapper
@Component
public interface LegalEntityMapper {
    /**
     * Maps compositon LegalEntity to stream LegalEntity model.
     *
     * @param legalEntity Integration legal entity
     * @return Stream legal entity
     */
    com.backbase.stream.legalentity.model.LegalEntity mapIntegrationToStream(
            com.backbase.stream.compositions.integration.legalentity.model.LegalEntity legalEntity);

    /**
     * Maps integration LegalEntity to compositon LegalEntity model.
     *
     * @param legalEntity Integration legal entity
     * @return Composition legal entity
     */
    com.backbase.stream.compositions.legalentity.model.LegalEntity mapIntegrationToCompostion(
            com.backbase.stream.compositions.integration.legalentity.model.LegalEntity legalEntity);

    /**
     * Maps composition LegalEntity to stream LegalEntity model.
     *
     * @param legalEntity Composition legal entity
     * @return Stream legal entity
     */
    com.backbase.stream.legalentity.model.LegalEntity mapCompostionToStream(
            com.backbase.stream.compositions.legalentity.model.LegalEntity legalEntity);

    /**
     * Maps stream LegalEntity to composition LegalEntity model.
     *
     * @param legalEntity Stream legal entity
     * @return Composition legal entity
     */
    com.backbase.stream.compositions.legalentity.model.LegalEntity mapStreamToComposition(
            com.backbase.stream.legalentity.model.LegalEntity legalEntity);

    /**
     * Maps event LegalEntity to stream LegalEntity model.
     *
     * @param legalEntity Event legal entity
     * @return Stream legal entity
     */
    com.backbase.stream.legalentity.model.LegalEntity mapEventToStream(
            com.backbase.com.backbase.stream.compositions.events.ingress.event.spec.v1.LegalEntity legalEntity);


}
