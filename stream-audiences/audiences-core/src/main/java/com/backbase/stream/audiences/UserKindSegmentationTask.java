package com.backbase.stream.audiences;

import com.backbase.audiences.collector.api.service.v1.model.CustomerOnboardedRequest;
import com.backbase.stream.worker.model.StreamTask;
import lombok.Data;

@Data
public class UserKindSegmentationTask extends StreamTask {

    private CustomerOnboardedRequest customerOnboardedRequest;

    @Override
    public String getName() {
        return "customersSegment";
    }
}
