package com.backbase.stream;

import com.backbase.stream.legalentity.model.ServiceAgreementV2;
import com.backbase.stream.product.task.BatchProductIngestionMode;
import com.backbase.stream.worker.model.StreamTask;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
public class ServiceAgreementTaskV2 extends StreamTask {

    private ServiceAgreementV2 serviceAgreement;
    private BatchProductIngestionMode ingestionMode;

    public ServiceAgreementTaskV2(ServiceAgreementV2 data) {
        super(data.getExternalId());
        this.serviceAgreement = data;
        this.ingestionMode = BatchProductIngestionMode.UPSERT;
    }

    public ServiceAgreementTaskV2(ServiceAgreementV2 data, BatchProductIngestionMode ingestionMode) {
        super(data.getExternalId());
        this.serviceAgreement = data;
        this.ingestionMode = ingestionMode;
    }

    @Override
    public String getName() {
        return serviceAgreement.getExternalId();
    }
}
