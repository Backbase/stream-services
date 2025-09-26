package com.backbase.stream.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.backbase.accesscontrol.legalentity.api.integration.v3.model.LegalEntityItem;
import com.backbase.accesscontrol.legalentity.api.integration.v3.model.SingleServiceAgreement;
import com.backbase.accesscontrol.legalentity.api.integration.v3.model.Status;
import com.backbase.accesscontrol.legalentity.api.service.v1.model.LegalEntityUpdate;
import com.backbase.accesscontrol.legalentity.api.service.v1.model.LegalEntityWithParent;
import com.backbase.stream.legalentity.model.CustomerCategory;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.LegalEntityStatus;
import com.backbase.stream.legalentity.model.LegalEntityType;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class LegalEntityMapperTest {

    public static final LegalEntity LEGAL_ENTITY_MODEL = new LegalEntity()
        .name("Test Legal Entity")
        .externalId("externalId")
        .internalId("internalId")
        .legalEntityType(com.backbase.stream.legalentity.model.LegalEntityType.BANK)
        .customerCategory(com.backbase.stream.legalentity.model.CustomerCategory.RETAIL)
        .parentExternalId("parentExternalId")
        .activateSingleServiceAgreement(null)
        .additions(Map.of("k1", "v1", "k2", "v2"));

    private final LegalEntityMapper mapper = Mappers.getMapper(LegalEntityMapper.class);

    @Test
    void toPresentationWithNullArg() {
        assertNull(mapper.toPresentation((LegalEntity) null));
    }

    @Test
    void toPresentationShouldCopyAllAttributes() {
        LegalEntityItem presentation = mapper.toPresentation(LEGAL_ENTITY_MODEL);

        assertAll(
            () -> assertNotNull(presentation),
            () -> assertEquals("externalId", presentation.getExternalId()),
            () -> assertEquals("Test Legal Entity", presentation.getName()),
            () -> assertEquals(com.backbase.accesscontrol.legalentity.api.integration.v3.model.LegalEntityType.BANK,
                presentation.getType()
            ),
            () -> assertEquals(
                com.backbase.accesscontrol.legalentity.api.integration.v3.model.CustomerCategory.RETAIL,
                presentation.getCustomerCategory()
            ),
            () -> assertEquals("parentExternalId", presentation.getParentExternalId()),
            () -> assertEquals(Boolean.FALSE, presentation.getCreateSingleServiceAgreement())
        );
    }

    @Test
    void toStreamFromLegalEntityItemBaseShouldCopyAllAttributes() {
        var legalEntityItemBase = new com.backbase.accesscontrol.legalentity.api.integration.v3.model.LegalEntity()
            .name("Test Legal Entity")
            .id("internalId")
            .externalId("externalId")
            .type(com.backbase.accesscontrol.legalentity.api.integration.v3.model.LegalEntityType.CUSTOMER)
            .customerCategory(com.backbase.accesscontrol.legalentity.api.integration.v3.model.CustomerCategory.BUSINESS)
            .additions(Map.of("k1", "v1", "k2", "v2"));
        LegalEntity model = mapper.toStream(legalEntityItemBase);
        assertAll(
            () -> assertNotNull(model),
            () -> assertEquals("externalId", model.getExternalId()),
            () -> assertEquals("internalId", model.getInternalId()),
            () -> assertEquals("Test Legal Entity", model.getName()),
            () -> assertEquals(LegalEntityType.CUSTOMER, model.getLegalEntityType()),
            () -> assertEquals(CustomerCategory.BUSINESS, model.getCustomerCategory())
        );

    }

    @Test
    void toStreamFromLegalEntityItemShouldCopyAllAttributes() {
        LegalEntityWithParent legalEntityItem = new LegalEntityWithParent()
            .id("internalId")
            .externalId("externalId")
            .parentId("parentId")
            .name("Test Legal Entity")
            .type(com.backbase.accesscontrol.legalentity.api.service.v1.model.LegalEntityType.CUSTOMER);
        LegalEntity model = mapper.toStream(legalEntityItem);
        assertAll(
            () -> assertNotNull(model),
            () -> assertEquals("externalId", model.getExternalId()),
            () -> assertEquals("internalId", model.getInternalId()),
            () -> assertEquals("parentId", model.getParentInternalId()),
            () -> assertEquals("Test Legal Entity", model.getName()),
            () -> assertEquals(LegalEntityType.CUSTOMER, model.getLegalEntityType())
        );
    }

    @Test
    void toStreamFromGetServiceAgreementShouldCopyAllAttributes() {
        SingleServiceAgreement getServiceAgreement = new SingleServiceAgreement()
            .id("internalId")
            .externalId("externalId")
            .name("Test Service Agreement")
            .description("description")
            .status(Status.ENABLED)
            .creatorLegalEntity("creatorLegalEntity");
        ServiceAgreement model = mapper.toStream(getServiceAgreement);
        assertAll(
            () -> assertNotNull(model),
            () -> assertEquals("externalId", model.getExternalId()),
            () -> assertEquals("internalId", model.getInternalId()),
            () -> assertEquals("Test Service Agreement", model.getName()),
            () -> assertEquals("description", model.getDescription()),
            () -> assertEquals(Boolean.TRUE, model.getIsMaster()),
            () -> assertEquals(LegalEntityStatus.ENABLED, model.getStatus())
        );
    }

    @Test
    void testToLegalEntityPutWithNullArg() {
        assertNull(mapper.toLegalEntityPut((LegalEntity) null));
    }

    @Test
    void testToLegalEntityPutShouldCopyAllAttributes() {
        LegalEntityUpdate presentation = mapper.toLegalEntityPut(LEGAL_ENTITY_MODEL);

        assertAll(
            () -> assertNotNull(presentation),
            () -> assertEquals("externalId", presentation.getExternalId()),
            () -> assertEquals("Test Legal Entity", presentation.getName()),
            () -> assertEquals(
                com.backbase.accesscontrol.legalentity.api.service.v1.model.LegalEntityType.BANK,
                presentation.getType()
            ),
            () -> assertEquals(
                com.backbase.accesscontrol.legalentity.api.service.v1.model.CustomerCategory.RETAIL,
                presentation.getCustomerCategory()
            )
        );
    }
}
