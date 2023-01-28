package com.backbase.stream.compositions.product.core.model;

import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties;
import com.backbase.stream.compositions.product.util.JsonUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;


class RequestConfigTest {

    @Test
    void testRequestConfig() {
        RequestConfig request = getRequestConfig();
        RequestConfig.Chains chains = request.getChains();
        Assertions.assertNotNull(chains.getTransactionComposition());
        Assertions.assertEquals(chains.getTransactionComposition().getExcludeProductTypeExternalIds().size(),0);
        Assertions.assertEquals(request.isTransactionChainAsync().get(),false);
        Assertions.assertEquals(request.isTransactionChainEnabled().get(),true);
        Assertions.assertNotNull(chains.getPaymentOrderComposition());
        Assertions.assertEquals(chains.getPaymentOrderComposition().getExcludeProductTypeExternalIds().size(),0);
        Assertions.assertEquals(request.isPaymentOrderChainAsync().get(),false);
        Assertions.assertEquals(request.isPaymentOrderChainEnabled().get(),true);


    }

    @Test
    void testEmptyRequestConfig() {
        RequestConfig request = new RequestConfig();
        request.setChains(new RequestConfig.Chains());
        Assertions.assertNull(request.getChains().getTransactionComposition());
        Assertions.assertEquals(request.isTransactionChainAsync(), Optional.empty());
        Assertions.assertEquals(request.isTransactionChainEnabled(), Optional.empty());
        Assertions.assertNull(request.getChains().getPaymentOrderComposition());
        Assertions.assertEquals(request.isPaymentOrderChainAsync(), Optional.empty());
        Assertions.assertEquals(request.isPaymentOrderChainEnabled(), Optional.empty());
    }

    private RequestConfig getRequestConfig(){
        return  JsonUtil.readJsonFileToObject(RequestConfig.class,"integration-data/request-config.json");
    }

}