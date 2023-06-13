package com.backbase.stream;

import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.product.task.BatchProductIngestionMode;
import com.backbase.stream.worker.model.StreamTask;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
public class LegalEntityTask extends StreamTask {

    private LegalEntity legalEntity;
    private BatchProductIngestionMode ingestionMode;

    public LegalEntityTask(LegalEntity data) {
        super(data.getExternalId());
        this.legalEntity = data;
        this.ingestionMode = BatchProductIngestionMode.UPSERT;
    }

    public LegalEntityTask(LegalEntity data, BatchProductIngestionMode ingestionMode) {
        super(data.getExternalId());
        this.legalEntity = data;
        this.ingestionMode = ingestionMode;
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
