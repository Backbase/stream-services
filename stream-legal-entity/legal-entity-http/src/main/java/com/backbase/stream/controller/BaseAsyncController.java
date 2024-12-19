package com.backbase.stream.controller;

import com.backbase.stream.LegalEntityTask;
import com.backbase.stream.UpdatedServiceAgreementTask;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.UpdatedServiceAgreement;
import com.backbase.stream.mapper.UnitOfWorkMapper;
import com.backbase.stream.worker.model.UnitOfWork;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.factory.Mappers;

abstract class BaseAsyncController {

    protected final UnitOfWorkMapper unitOfWorkMapper = Mappers.getMapper(UnitOfWorkMapper.class);

    protected UnitOfWork<LegalEntityTask> createUnitOfWork(List<LegalEntity> legalEntities) {
        List<LegalEntityTask> tasks = legalEntities.stream()
            .map(LegalEntityTask::new)
            .collect(Collectors.toList());
        return UnitOfWork.from("http-" + System.currentTimeMillis(), tasks);
    }

    protected UnitOfWork<UpdatedServiceAgreementTask> createServiceAgreementUnitOfWork(
        List<UpdatedServiceAgreement> serviceAgreements) {
        List<UpdatedServiceAgreementTask> tasks = serviceAgreements.stream()
            .map(UpdatedServiceAgreementTask::new)
            .collect(Collectors.toList());
        return UnitOfWork.from("http-" + System.currentTimeMillis(), tasks);
    }

}
