package com.backbase.stream.mapper;

import com.backbase.stream.LegalEntityTask;
import com.backbase.stream.UpdatedServiceAgreementTask;
import com.backbase.stream.legalentity.model.LegalEntityResponse;
import com.backbase.stream.legalentity.model.UpdatedServiceAgreementResponse;
import com.backbase.stream.worker.model.UnitOfWork;
import org.mapstruct.Mapper;

@Mapper
public interface UnitOfWorkMapper {

    LegalEntityResponse convertToLegalEntityResponse(UnitOfWork<LegalEntityTask> unitOfWork);

    UpdatedServiceAgreementResponse convertToUpdatedServiceAgreementResponse(
        UnitOfWork<UpdatedServiceAgreementTask> unitOfWork);
}
