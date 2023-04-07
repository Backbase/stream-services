package com.backbase.stream;

import com.backbase.stream.legalentity.model.UpdatedServiceAgreement;
import com.backbase.stream.worker.model.StreamTask;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
public class UpdatedServiceAgreementTask extends StreamTask {

    private UpdatedServiceAgreement serviceAgreement;

    public UpdatedServiceAgreementTask(UpdatedServiceAgreement serviceAgreement) {
        super(serviceAgreement.getExternalId());
        this.serviceAgreement = serviceAgreement;
    }

    @Override
    public String getName() {
        return serviceAgreement.getExternalId();
    }

    public UpdatedServiceAgreement getData() {
        return serviceAgreement;
    }

    public void setData(UpdatedServiceAgreement serviceAgreement) {
        this.serviceAgreement = serviceAgreement;
    }
}
