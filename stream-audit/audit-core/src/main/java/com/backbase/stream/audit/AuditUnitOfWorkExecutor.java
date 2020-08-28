package com.backbase.stream.audit;

import com.backbase.dbs.audit.service.model.AuditMessage;
import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.UnitOfWorkExecutor;
import com.backbase.stream.worker.configuration.StreamWorkerConfiguration;
import com.backbase.stream.worker.model.UnitOfWork;
import com.backbase.stream.worker.repository.UnitOfWorkRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuditUnitOfWorkExecutor extends UnitOfWorkExecutor<AuditMessagesTask> {


    public AuditUnitOfWorkExecutor(
        UnitOfWorkRepository<AuditMessagesTask, String> repository,
        StreamTaskExecutor<AuditMessagesTask> streamTaskExecutor,
        StreamWorkerConfiguration streamWorkerConfiguration) {
        super(repository, streamTaskExecutor, streamWorkerConfiguration);
    }

    public UnitOfWork<AuditMessagesTask> prepareUnitOfWork(List<AuditMessage> items) {
        UnitOfWork<AuditMessagesTask> unitOfWork = new UnitOfWork<>();
        unitOfWork.setUnitOfOWorkId("audit-messages-" + LocalDateTime.now());
        unitOfWork.setStreamTasks(new ArrayList<>());
        int partitionSize = streamWorkerConfiguration.getBufferSize();

        for (int i = 0; i < items.size(); i += partitionSize) {
            List<AuditMessage> messages = items.subList(i, Math.min(i + partitionSize, items.size()));
            String taskId = unitOfWork.getUnitOfOWorkId() + "-" + i + 1;
            AuditMessagesTask auditMessagesTask = new AuditMessagesTask(taskId, messages);
            unitOfWork.getStreamTasks().add(auditMessagesTask);
        }
        return unitOfWork;
    }
}