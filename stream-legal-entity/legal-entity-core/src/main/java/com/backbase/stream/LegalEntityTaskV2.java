package com.backbase.stream;

import com.backbase.stream.legalentity.model.LegalEntityV2;
import com.backbase.stream.product.task.BatchProductIngestionMode;
import com.backbase.stream.worker.model.StreamTask;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
public class LegalEntityTaskV2 extends StreamTask {

  private LegalEntityV2 legalEntityV2;
  private BatchProductIngestionMode ingestionMode;

  public LegalEntityTaskV2(LegalEntityV2 data) {
    super(data.getExternalId());
    this.legalEntityV2 = data;
    this.ingestionMode = BatchProductIngestionMode.UPSERT;
  }

  public LegalEntityTaskV2(LegalEntityV2 data, BatchProductIngestionMode ingestionMode) {
    super(data.getExternalId());
    this.legalEntityV2 = data;
    this.ingestionMode = ingestionMode;
  }

  public LegalEntityV2 getData() {
    return legalEntityV2;
  }

  public LegalEntityTaskV2 data(LegalEntityV2 legalEntity) {
    this.legalEntityV2 = legalEntity;
    return this;
  }

  @Override
  public String getName() {
    return legalEntityV2.getExternalId();
  }
}
