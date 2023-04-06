package com.backbase.stream.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.backbase.dbs.limit.api.service.v2.model.CreateLimitRequestBody;
import com.backbase.dbs.limit.api.service.v2.model.Entity;
import com.backbase.dbs.limit.api.service.v2.model.PeriodicLimitsBounds;
import com.backbase.dbs.limit.api.service.v2.model.TransactionalLimitsBound;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class LimitsMapperTest {

    private final LimitsMapper mapper = new LimitsMapperImpl();

    @Test
    void testLimitsRetrievalPostRequestBodyMapping() {

        // When
        var result = mapper.map(createLimitRequestBody());

        // Then
        assertNotNull(result.getLimitsRetrievalOptions());
        assertEquals(1, result.getLimitsRetrievalOptions().size());
        assertNotNull(result.getLimitsRetrievalOptions().get(0).getUserBBID());
        assertNotNull(result.getLimitsRetrievalOptions().get(0).getShadow());
        assertNotNull(result.getLimitsRetrievalOptions().get(0).getLookupKeys());
        assertEquals(4, result.getLimitsRetrievalOptions().get(0).getLookupKeys().size());
    }

    @Test
    void testUpdateLimitRequestBodyMapping() {

        // When
        var result = mapper.mapUpdateLimits(createLimitRequestBody());

        // Then
        assertNotNull(result);
        assertEquals("USD", result.getCurrency());
        assertNotNull(result.getPeriodicLimitsBounds());
        assertNotNull(result.getTransactionalLimitsBound());
        assertEquals(BigDecimal.TEN, result.getTransactionalLimitsBound().getAmount());
        assertEquals(BigDecimal.TEN, result.getPeriodicLimitsBounds().getDaily());
    }

    private CreateLimitRequestBody createLimitRequestBody() {
        var saEntity = new Entity().etype("SA").eref("internalSaId");
        var fagEntity = new Entity().etype("FAG").eref("internalFagId");
        var funEntity = new Entity().etype("FUN").eref("1018");
        var prvEntity = new Entity().etype("PRV").eref("approve");
        var createLimitRequestBody = new CreateLimitRequestBody()
            .periodicLimitsBounds(new PeriodicLimitsBounds().daily(BigDecimal.TEN))
            .transactionalLimitsBound(new TransactionalLimitsBound().amount(BigDecimal.TEN));
        createLimitRequestBody.entities(List.of(saEntity, fagEntity, funEntity, prvEntity));
        createLimitRequestBody.setUserBBID("internalUserId");
        createLimitRequestBody.currency("USD");
        return createLimitRequestBody;
    }

}
