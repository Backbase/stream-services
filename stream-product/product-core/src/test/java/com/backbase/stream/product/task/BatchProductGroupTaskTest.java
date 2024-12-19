package com.backbase.stream.product.task;

import com.backbase.stream.legalentity.model.BaseProductGroup;
import com.backbase.stream.legalentity.model.BatchProductGroup;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BatchProductGroupTaskTest {

  private final BatchProductGroup batchProductGroup =
      new BatchProductGroup()
          .serviceAgreement(new ServiceAgreement().internalId("sa_int_id").externalId("sa_ext_id"))
          .addProductGroupsItem(new BaseProductGroup().internalId("bpg_id_1"))
          .addProductGroupsItem(new BaseProductGroup().internalId("bpg_id_2"));

  @Test
  void create_BatchProductGroup() {
    BatchProductGroupTask batchProductGroupTask =
        new BatchProductGroupTask("bpg_task_id", batchProductGroup);
    Assertions.assertEquals("bpg_task_id", batchProductGroupTask.getName());
    Assertions.assertEquals(
        BatchProductIngestionMode.UPSERT, batchProductGroupTask.getIngestionMode());
    Assertions.assertEquals(
        "sa_int_id", batchProductGroupTask.getData().getServiceAgreement().getInternalId());
    Assertions.assertEquals(2, batchProductGroupTask.getData().getProductGroups().size());
  }

  @Test
  void create_BatchProductGroup_IngestionMode() {
    BatchProductGroupTask batchProductGroupTask =
        new BatchProductGroupTask(
            "bpg_task_id", batchProductGroup, BatchProductIngestionMode.REPLACE);
    Assertions.assertEquals("bpg_task_id", batchProductGroupTask.getName());
    Assertions.assertEquals(
        BatchProductIngestionMode.REPLACE, batchProductGroupTask.getIngestionMode());
    Assertions.assertEquals(
        "sa_int_id", batchProductGroupTask.getData().getServiceAgreement().getInternalId());
    Assertions.assertEquals(2, batchProductGroupTask.getData().getProductGroups().size());
  }

  @Test
  void set_BatchProductGroup_Data() {
    BatchProductGroupTask batchProductGroupTask = new BatchProductGroupTask();
    Assertions.assertNull(batchProductGroupTask.getBatchProductGroup());
    Assertions.assertNull(batchProductGroupTask.getName());
    Assertions.assertEquals(
        BatchProductIngestionMode.UPSERT, batchProductGroupTask.getIngestionMode());

    batchProductGroupTask = batchProductGroupTask.data(batchProductGroup);
    batchProductGroupTask.setId("bpg_task_id");
    Assertions.assertEquals("bpg_task_id", batchProductGroupTask.getName());
    Assertions.assertEquals(
        "sa_int_id", batchProductGroupTask.getData().getServiceAgreement().getInternalId());
    Assertions.assertEquals(2, batchProductGroupTask.getData().getProductGroups().size());
  }
}
