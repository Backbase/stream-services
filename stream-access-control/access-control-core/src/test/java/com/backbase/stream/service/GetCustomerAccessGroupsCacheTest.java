package com.backbase.stream.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.accesscontrol.customeraccessgroup.api.service.v1.CustomerAccessGroupApi;
import com.backbase.accesscontrol.customeraccessgroup.api.service.v1.model.CustomerAccessGroupItem;
import com.backbase.accesscontrol.customeraccessgroup.api.service.v1.model.GetCustomerAccessGroups;
import com.backbase.stream.configuration.CustomerAccessGroupCacheTestConfig;
import com.backbase.stream.worker.model.StreamTask;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {CustomerAccessGroupService.class, CustomerAccessGroupCacheTestConfig.class})
@EnableCaching
public class GetCustomerAccessGroupsCacheTest {

    @Autowired
    private CustomerAccessGroupService service;

    @Autowired
    private CustomerAccessGroupApi customerAccessGroupApi;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCache() {
        cacheManager.getCache("customer-access-group").clear();
    }

    @Test
    void testGetCustomerAccessGroupsFromCache() {
        String saName = "someSaName";
        String saDesc = "someSaDesc";
        StreamTask streamTask = Mockito.mock(StreamTask.class);

        CustomerAccessGroupItem cag1Item = new CustomerAccessGroupItem().id(1L).name(saName).description(saDesc)
            .mandatory(true);
        CustomerAccessGroupItem cag2Item = new CustomerAccessGroupItem().id(2L).name(saName + "1")
            .description(saDesc + "1").mandatory(false);

        GetCustomerAccessGroups getCustomerAccessGroups = new GetCustomerAccessGroups()
            .customerAccessGroups(List.of(cag1Item, cag2Item))
            .totalCount(2L);

        when(customerAccessGroupApi.getCustomerAccessGroups(any(), any(), any())).thenReturn(
            Mono.just(getCustomerAccessGroups));

        List<CustomerAccessGroupItem> itemsFirstAttempt = service.getCustomerAccessGroups(streamTask).block();
        List<CustomerAccessGroupItem> itemsSecondAttempt = service.getCustomerAccessGroups(streamTask).block();
        List<CustomerAccessGroupItem> itemsThirdAttempt = service.getCustomerAccessGroups(streamTask).block();

        assertThat(itemsFirstAttempt).containsExactlyInAnyOrder(cag1Item, cag2Item);
        assertThat(itemsFirstAttempt).isEqualTo(itemsSecondAttempt);
        assertThat(itemsSecondAttempt).isEqualTo(itemsThirdAttempt);
        verify(customerAccessGroupApi, times(1)).getCustomerAccessGroups(null, null, null);

    }

}
