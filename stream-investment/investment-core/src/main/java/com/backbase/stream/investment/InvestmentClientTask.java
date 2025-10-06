package com.backbase.stream.investment;

import com.backbase.investment.api.service.v1.model.ClientCreate;
import com.backbase.investment.api.service.v1.model.ClientCreateRequest;
import com.backbase.investment.api.service.v1.model.OASClient;
import com.backbase.investment.api.service.v1.model.OASClientUpdateRequest;
import com.backbase.investment.api.service.v1.model.PatchedOASClientUpdateRequest;
import com.backbase.stream.worker.model.StreamTask;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Stream task representing ingestion or update of an Investment Client.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class InvestmentClientTask extends StreamTask {

    public enum Operation { CREATE, PATCH, UPDATE }

    private Operation operation;
    private ClientCreateRequest createRequest;
    private PatchedOASClientUpdateRequest patchRequest;
    private OASClientUpdateRequest updateRequest;
    private UUID clientUuid; // Required for PATCH / UPDATE; populated after CREATE

    private ClientCreate createdClient; // response after CREATE
    private OASClient updatedClient; // response after PATCH / UPDATE

    public InvestmentClientTask(String id, Operation operation) {
        super(id);
        this.operation = operation;
    }

    @Override
    public String getName() {
        return getId();
    }
}

