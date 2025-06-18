package com.backbase.stream;

import com.backbase.accesscontrol.customeraccessgroup.api.service.v1.model.CustomerAccessGroupItem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CustomerAccessGroupTaskTest {

    private final CustomerAccessGroupItem customerAccessGroupItem = new CustomerAccessGroupItem()
        .id(1L).name("cagName").description("cagDescription").mandatory(true);

    @Test
    void getCustomerAccessGroupName() {
        CustomerAccessGroupTask task = new CustomerAccessGroupTask(customerAccessGroupItem);
        Assertions.assertEquals("cagName", task.getName());
    }

}
