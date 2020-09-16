package com.backbase.stream;

import com.backbase.dbs.limit.service.api.LimitsApi;
import com.backbase.dbs.limit.service.model.CreateLimitRequest;
import com.backbase.dbs.limit.service.model.CreateLimitResponse;
import com.backbase.dbs.user.presentation.service.api.UsersApi;
import com.backbase.stream.limit.LimitsSaga;
import com.backbase.stream.limit.LimitsTask;
import com.backbase.stream.limit.LimitsUnitOfWorkExecutor;
import com.backbase.stream.worker.model.UnitOfWork;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class LimitsService {

    private LimitsSaga limitsSaga;
    private LimitsApi limitsApi;
    private UsersApi usersApi;
    private LimitsUnitOfWorkExecutor limitsUnitOfWorkExecutor;

    public LimitsService(
            LimitsSaga limitsSaga,
            LimitsApi limitsApi,
            UsersApi usersApi,
            LimitsUnitOfWorkExecutor limitsUnitOfWorkExecutor
    ) {
        this.limitsSaga = limitsSaga;
        this.limitsApi = limitsApi;
        this.usersApi = usersApi;
        this.limitsUnitOfWorkExecutor = limitsUnitOfWorkExecutor;
    }

    public Flux<CreateLimitResponse> createUserLimits(Flux<CreateLimitRequest> items) {
        Flux<CreateLimitRequest> cleanItems = items.map(item -> {
            if (isUUID(item.getUserBBID())) {
                usersApi.getExternalIdByExternalIdgetUserByExternalid(item.getUserBBID()).subscribe(userItem -> {
                    item.setUserBBID(userItem.getId());
                });
            }
            return item;
        });
        Flux<UnitOfWork<LimitsTask>> unitOfWorkFlux = limitsUnitOfWorkExecutor.prepareUnitOfWork(cleanItems);
        return unitOfWorkFlux.flatMap(limitsUnitOfWorkExecutor::executeUnitOfWork).flatMap(limitsTaskUnitOfWork -> {
            Stream<CreateLimitResponse> stream = limitsTaskUnitOfWork.getStreamTasks().stream().map(LimitsTask::getResponse);
            return Flux.fromStream(stream);
        });
    }

    private boolean isUUID(String input) {
        Pattern pattern = Pattern.compile("/^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/");
        return pattern.matcher(input).matches();
    }
}
