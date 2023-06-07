package com.backbase.stream.audiences;

import com.backbase.stream.worker.model.StreamTask;
import lombok.Data;

@Data
public class CustomersSegmentTask extends StreamTask {

    @Override
    public String getName() {
        return "customersSegment";
    }
}
