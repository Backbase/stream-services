package com.backbase.stream.compositions.product.core.mapper;

import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

/**
 * This is a mapper for ProductGroup objects used in:
 * - stream-models/legal-entity-model
 * - product-composition-api
 * - product-integration-api
 * - product-events
 * <p>
 * All ProductGroup objects used in above modules have exactly same structures they are built
 * from the common /api folder.
 */
@Mapper
@Component
public interface ProductGroupMapper {
    /**
     * Maps composition ProductGroup to stream ProductGroup model.
     *
     * @param productGroup Integration product group
     * @return Stream product group
     */
    com.backbase.stream.legalentity.model.ProductGroup mapIntegrationToStream(
            com.backbase.stream.compositions.integration.product.model.ProductGroup productGroup);

    /**
     * Maps integration ProductGroup to composition ProductGroup model.
     *
     * @param productGroup Integration product group
     * @return Composition product group
     */
    com.backbase.stream.compositions.product.model.ProductGroup mapIntegrationToComposition(
            com.backbase.stream.compositions.integration.product.model.ProductGroup productGroup);

    /**
     * Maps composition ProductGroup to stream ProductGroup model.
     *
     * @param productGroup Composition product group
     * @return Stream product group
     */
    com.backbase.stream.legalentity.model.ProductGroup mapCompositionToStream(
            com.backbase.stream.compositions.product.model.ProductGroup productGroup);

    /**
     * Maps stream ProductGroup to composition ProductGroup model.
     *
     * @param productGroup Stream product group
     * @return Composition product group
     */
    com.backbase.stream.compositions.product.model.ProductGroup mapStreamToComposition(
            com.backbase.stream.legalentity.model.ProductGroup productGroup);

    /**
     * Maps event ProductGroup to stream ProductGroup model.
     *
     * @param productGroup Event product group
     * @return Stream product group
     */
    com.backbase.stream.legalentity.model.ProductGroup mapEventToStream(
            com.backbase.com.backbase.stream.compositions.events.ingress.event.spec.v1.ProductGroup productGroup);

    /**
     * Maps steam ProductGroup to event ProductGroup model.
     *
     * @param productGroup Stream product group
     * @return Event product group
     */
    com.backbase.com.backbase.stream.compositions.events.egress.event.spec.v1.ProductGroup mapStreamToEvent(
            com.backbase.stream.legalentity.model.ProductGroup productGroup);
}
