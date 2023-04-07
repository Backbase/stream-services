package com.backbase.stream.product.task;

import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.legalentity.model.ServiceAgreement;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ProductGroupTaskTest {

    private final ProductGroup productGroup =
            new ProductGroup()
                    .serviceAgreement(
                            new ServiceAgreement().internalId("sa_int_id").externalId("sa_ext_id"));

    @Test
    void create_ProductGroup() {
        ProductGroupTask productGroupTask = new ProductGroupTask(productGroup);
        Assertions.assertNull(productGroupTask.getName());
        Assertions.assertEquals(
                "sa_int_id", productGroupTask.getData().getServiceAgreement().getInternalId());
    }

    @Test
    void create_ProductGroup_Id() {
        ProductGroupTask productGroupTask = new ProductGroupTask("pg_task_id", productGroup);
        Assertions.assertEquals("pg_task_id", productGroupTask.getName());
        Assertions.assertEquals(
                "sa_int_id", productGroupTask.getData().getServiceAgreement().getInternalId());
    }

    @Test
    void set_ProductGroup_Data() {
        ProductGroupTask productGroupTask = new ProductGroupTask();
        Assertions.assertNull(productGroupTask.getProductGroup());
        Assertions.assertNull(productGroupTask.getName());

        productGroupTask = productGroupTask.data(productGroup);
        productGroupTask.setId("pg_task_id");
        Assertions.assertEquals("pg_task_id", productGroupTask.getName());
        Assertions.assertEquals(
                "sa_int_id", productGroupTask.getData().getServiceAgreement().getInternalId());
    }
}
