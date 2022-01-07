package com.backbase.stream.compositions.productcatalog.mapper;

import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

/**
 * This is a mapper for ProductCatalog objects used in:
 * - stream-models/product-catalog-model
 * - product-catalog-composition-api
 * - product-catalog-integration-api
 * - product-catalog-events
 * <p>
 * All ProductCatalog objects used in above modules have exactly same structures they are built
 * from the common /api folder.
 */

@Component
@Mapper(componentModel = "spring")
public interface ProductCatalogMapper {
    /**
     * Maps composition ProductCatalog to stream ProductCatalog model.
     *
     * @param productCatalog Integration product catalog
     * @return Stream product catalog
     */
    com.backbase.stream.productcatalog.model.ProductCatalog mapIntegrationToStream(
            com.backbase.stream.compositions.integration.productcatalog.model.ProductCatalog productCatalog);

    /**
     * Maps integration ProductCatalog to composition ProductCatalog model.
     *
     * @param productCatalog Integration product catalog
     * @return Composition product catalog
     */
    com.backbase.stream.compositions.productcatalog.model.ProductCatalog mapIntegrationToComposition(
            com.backbase.stream.compositions.integration.productcatalog.model.ProductCatalog productCatalog);

    /**
     * Maps composition ProductCatalog to stream ProductCatalog model.
     *
     * @param productCatalog Composition product catalog
     * @return Stream product catalog
     */
    com.backbase.stream.productcatalog.model.ProductCatalog mapCompositionToStream(
            com.backbase.stream.compositions.productcatalog.model.ProductCatalog productCatalog);

    /**
     * Maps stream ProductCatalog to composition ProductCatalog model.
     *
     * @param productCatalog Stream product catalog
     * @return Composition product catalog
     */
    com.backbase.stream.compositions.productcatalog.model.ProductCatalog mapStreamToComposition(
            com.backbase.stream.productcatalog.model.ProductCatalog productCatalog);

    /**
     * Maps event ProductCatalog to stream ProductCatalog model.
     *
     * @param productCatalog Event product catalog
     * @return Stream product catalog
     */
    com.backbase.stream.productcatalog.model.ProductCatalog mapEventToStream(
            com.backbase.com.backbase.stream.compositions.events.ingress.event.spec.v1.ProductCatalog productCatalog);

    /**
     * Maps steam ProductCatalog to event ProductCatalog model.
     *
     * @param productCatalog Stream product catalog
     * @return Event product catalog
     */
    com.backbase.com.backbase.stream.compositions.events.egress.event.spec.v1.ProductCatalog mapStreamToEvent(
            com.backbase.stream.productcatalog.model.ProductCatalog productCatalog);
}
