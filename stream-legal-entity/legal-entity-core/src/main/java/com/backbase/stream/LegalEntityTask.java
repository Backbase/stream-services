package com.backbase.stream;

import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.worker.model.StreamTask;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
public class LegalEntityTask extends StreamTask {

  private LegalEntity legalEntity;

  public LegalEntityTask(LegalEntity data) {
    super(data.getExternalId());
    this.legalEntity = data;
  }

  public LegalEntity getData() {
    return legalEntity;
  }

  public LegalEntityTask data(LegalEntity legalEntity) {
    this.legalEntity = legalEntity;
    return this;
  }

  @Override
  public String getName() {
    return legalEntity.getExternalId();
  }
}
