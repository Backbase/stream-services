package com.backbase.stream.product.task;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BatchProductIngestionModeTest {
  @Test
  void testUpsertMode() {
    BatchProductIngestionMode ingestionMode = BatchProductIngestionMode.builder().build();

    assertFalse(ingestionMode.isFunctionGroupsReplaceEnabled());
    assertFalse(ingestionMode.isDataGroupsReplaceEnabled());
    assertFalse(ingestionMode.isArrangementsReplaceEnabled());

    ingestionMode = BatchProductIngestionMode.UPSERT;

    assertFalse(ingestionMode.isFunctionGroupsReplaceEnabled());
    assertFalse(ingestionMode.isDataGroupsReplaceEnabled());
    assertFalse(ingestionMode.isArrangementsReplaceEnabled());
  }

  @Test
  void testReplaceMode() {
    BatchProductIngestionMode ingestionMode = BatchProductIngestionMode.REPLACE;

    assertTrue(ingestionMode.isFunctionGroupsReplaceEnabled());
    assertTrue(ingestionMode.isDataGroupsReplaceEnabled());
    assertTrue(ingestionMode.isArrangementsReplaceEnabled());
  }

  @Test
  void testFunctionGroupsReplaceOnlyMode() {
    BatchProductIngestionMode ingestionMode =
        BatchProductIngestionMode.builder()
            .functionGroupsMode(BatchProductIngestionMode.FunctionGroupsMode.REPLACE)
            .build();

    assertTrue(ingestionMode.isFunctionGroupsReplaceEnabled());
    assertFalse(ingestionMode.isDataGroupsReplaceEnabled());
    assertFalse(ingestionMode.isArrangementsReplaceEnabled());
  }

  @Test
  void testDataGroupsReplaceOnlyMode() {
    BatchProductIngestionMode ingestionMode =
        BatchProductIngestionMode.builder()
            .dataGroupIngestionMode(BatchProductIngestionMode.DataGroupsMode.REPLACE)
            .build();

    assertFalse(ingestionMode.isFunctionGroupsReplaceEnabled());
    assertTrue(ingestionMode.isDataGroupsReplaceEnabled());
    assertFalse(ingestionMode.isArrangementsReplaceEnabled());
  }

  @Test
  void testArrangementsReplaceOnlyMode() {
    BatchProductIngestionMode ingestionMode =
        BatchProductIngestionMode.builder()
            .arrangementsMode(BatchProductIngestionMode.ArrangementsMode.REPLACE)
            .build();

    assertFalse(ingestionMode.isFunctionGroupsReplaceEnabled());
    assertFalse(ingestionMode.isDataGroupsReplaceEnabled());
    assertTrue(ingestionMode.isArrangementsReplaceEnabled());
  }
}
