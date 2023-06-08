package com.backbase.stream.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.backbase.dbs.accesscontrol.api.service.v3.model.GetServiceAgreement;
import com.backbase.dbs.accesscontrol.api.service.v3.model.LegalEntityCreateItem;
import com.backbase.dbs.accesscontrol.api.service.v3.model.LegalEntityItem;
import com.backbase.dbs.accesscontrol.api.service.v3.model.LegalEntityItemBase;
import com.backbase.dbs.accesscontrol.api.service.v3.model.LegalEntityPut;
import com.backbase.dbs.accesscontrol.api.service.v3.model.Status;
import com.backbase.stream.legalentity.model.CustomerCategory;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.LegalEntityStatus;
import com.backbase.stream.legalentity.model.LegalEntityType;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import java.time.LocalDate;
import java.time.Month;
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
        .activateSingleServiceAgreement(true)
        .additions(Map.of("k1", "v1", "k2", "v2"));

    private final LegalEntityMapper mapper = Mappers.getMapper(LegalEntityMapper.class);

    @Test
    void toPresentationWithNullArg() {
        assertNull(mapper.toPresentation(null));
    }

    @Test
    void toPresentationShouldCopyAllAttributes() {
        LegalEntityCreateItem presentation = mapper.toPresentation(LEGAL_ENTITY_MODEL);

        assertAll(
            () -> assertNotNull(presentation),
            () -> assertEquals("externalId", presentation.getExternalId()),
            () -> assertEquals("Test Legal Entity", presentation.getName()),
            () -> assertEquals(
                com.backbase.dbs.accesscontrol.api.service.v3.model.LegalEntityType.BANK,
                presentation.getType()
            ),
            () -> assertEquals(
                com.backbase.dbs.accesscontrol.api.service.v3.model.CustomerCategory.RETAIL,
                presentation.getCustomerCategory()
            ),
            () -> assertEquals("parentExternalId", presentation.getParentExternalId()),
            () -> assertEquals(Boolean.TRUE, presentation.getActivateSingleServiceAgreement())
        );
    }

    @Test
    void toStreamFromLegalEntityItemBaseWithNullArg() {
        assertNull(mapper.toStream((LegalEntityItemBase) null));
    }

    @Test
    void toStreamFromLegalEntityItemBaseShouldCopyAllAttributes() {
        LegalEntityItemBase legalEntityItemBase = new LegalEntityItemBase()
            .name("Test Legal Entity")
            .id("internalId")
            .externalId("externalId")
            .type(com.backbase.dbs.accesscontrol.api.service.v3.model.LegalEntityType.CUSTOMER)
            .customerCategory(com.backbase.dbs.accesscontrol.api.service.v3.model.CustomerCategory.BUSINESS)
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
    void toStreamFromLegalEntityItemWithNullArg() {
        assertNull(mapper.toStream((LegalEntityItem) null));
    }

    @Test
    void toStreamFromLegalEntityItemShouldCopyAllAttributes() {
        LegalEntityItem legalEntityItem = new LegalEntityItem()
            .id("internalId")
            .externalId("externalId")
            .parentId("parentId")
            .name("Test Legal Entity")
            .type(com.backbase.dbs.accesscontrol.api.service.v3.model.LegalEntityType.CUSTOMER)
            ;
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
    void toModelFromLegalEntityCreateItemWithNullArg() {
        assertNull(mapper.toModel(null));
    }

    @Test
    void toModelFromLegalEntityCreateItemShouldCopyAllAttributes() {
        LegalEntityCreateItem legalEntity = new LegalEntityCreateItem()
            .externalId("externalId")
            .parentExternalId("parentExternalId")
            .name("Test Legal Entity")
            .type(com.backbase.dbs.accesscontrol.api.service.v3.model.LegalEntityType.CUSTOMER)
            .activateSingleServiceAgreement(true);
        LegalEntity model = mapper.toModel(legalEntity);
        assertAll(
            () -> assertNotNull(model),
            () -> assertEquals("externalId", model.getExternalId()),
            () -> assertEquals("parentExternalId", model.getParentExternalId()),
            () -> assertEquals("Test Legal Entity", model.getName()),
            () -> assertEquals(LegalEntityType.CUSTOMER, model.getLegalEntityType()),
            () -> assertEquals(Boolean.TRUE, model.getActivateSingleServiceAgreement())
        );
    }

    @Test
    void toStreamFromGetServiceAgreementWithNullArg() {
        assertNull(mapper.toStream((GetServiceAgreement) null));
    }

    @Test
    void toStreamFromGetServiceAgreementShouldCopyAllAttributes() {
        GetServiceAgreement getServiceAgreement = new GetServiceAgreement()
            .id("internalId")
            .externalId("externalId")
            .name("Test Service Agreement")
            .description("description")
            .validFromDate("2018-08-31")
            .validFromTime("07:48:23")
            .validUntilDate("2018-09-30")
            .validUntilTime("07:49:24")
            .status(Status.ENABLED)
            .isMaster(true)
            .creatorLegalEntity("creatorLegalEntity");
        ServiceAgreement model = mapper.toStream(getServiceAgreement);
        assertAll(
            () -> assertNotNull(model),
            () -> assertEquals("externalId", model.getExternalId()),
            () -> assertEquals("internalId", model.getInternalId()),
            () -> assertEquals("Test Service Agreement", model.getName()),
            () -> assertEquals("description", model.getDescription()),
            () -> assertEquals(LocalDate.of(2018, Month.AUGUST, 31), model.getValidFromDate()),
            () -> assertEquals("07:48:23", model.getValidFromTime()),
            () -> assertEquals(LocalDate.of(2018, Month.SEPTEMBER, 30), model.getValidUntilDate()),
            () -> assertEquals("07:49:24", model.getValidUntilTime()),
            () -> assertEquals(Boolean.TRUE, model.getIsMaster()),
            () -> assertEquals(LegalEntityStatus.ENABLED, model.getStatus())
        );
    }

    @Test
    void testToLegalEntityPutWithNullArg() {
        assertNull(mapper.toLegalEntityPut(null));
    }

    @Test
    void testToLegalEntityPutShouldCopyAllAttributes() {
        LegalEntityPut presentation = mapper.toLegalEntityPut(LEGAL_ENTITY_MODEL);

        assertAll(
            () -> assertNotNull(presentation),
            () -> assertEquals("externalId", presentation.getCurrentExternalId()),
            () -> assertEquals("externalId", presentation.getNewValues().getExternalId()),
            () -> assertEquals("Test Legal Entity", presentation.getNewValues().getName()),
            () -> assertEquals(
                com.backbase.dbs.accesscontrol.api.service.v3.model.LegalEntityType.BANK,
                presentation.getNewValues().getType()
            ),
            () -> assertEquals("parentExternalId", presentation.getNewValues().getParentExternalId()),
            () -> assertEquals(Boolean.TRUE, presentation.getNewValues().getActivateSingleServiceAgreement())
        );
    }
}
