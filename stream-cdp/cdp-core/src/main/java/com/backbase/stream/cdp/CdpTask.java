package com.backbase.stream.cdp;

import com.backbase.cdp.ingestion.api.service.v1.model.CdpEvents;
import com.backbase.stream.worker.model.StreamTask;
import lombok.Data;

@Data
public class CdpTask extends StreamTask {

    private CdpEvents cdpEvents;

    @Override
    public String getName() {
        return "cdpProfilesIngestionTask";
    }
}
