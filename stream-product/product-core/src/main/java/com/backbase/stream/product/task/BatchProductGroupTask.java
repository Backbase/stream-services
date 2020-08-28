package com.backbase.stream.product.task;

import com.backbase.stream.legalentity.model.BatchProductGroup;
import com.backbase.stream.worker.model.StreamTask;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class BatchProductGroupTask extends StreamTask {

    private IngestionMode ingestionMode = IngestionMode.REPLACE;

    private BatchProductGroup batchProductGroup;

    public BatchProductGroupTask(String id, BatchProductGroup batchProductGroup, IngestionMode ingestionMode) {
        this(id, batchProductGroup);
        this.ingestionMode = ingestionMode;
    }

    public BatchProductGroupTask(String id, BatchProductGroup batchProductGroup) {
        super(id);
        this.batchProductGroup = batchProductGroup;
    }

    public BatchProductGroup getData() {
        return batchProductGroup;
    }


    public BatchProductGroupTask data(BatchProductGroup batchProductGroup) {
        this.batchProductGroup = batchProductGroup;
        return this;
    }

    public enum IngestionMode {
        UPDATE,
        REPLACE
    }

    @Override
    public String getName() {
        return getId();
    }
}
