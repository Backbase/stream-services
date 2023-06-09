package com.backbase.stream;

import com.backbase.dbs.limit.api.service.v2.model.CreateLimitRequestBody;
import com.backbase.dbs.limit.api.service.v2.model.LimitsPostResponseBody;
import com.backbase.dbs.user.api.service.v2.UserManagementApi;
import com.backbase.stream.limit.LimitsTask;
import com.backbase.stream.limit.LimitsUnitOfWorkExecutor;
import com.backbase.stream.worker.model.UnitOfWork;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import reactor.core.publisher.Flux;

public class LimitsService {

    private UserManagementApi userManagementApi;
    private LimitsUnitOfWorkExecutor limitsUnitOfWorkExecutor;

    public LimitsService(
        UserManagementApi userManagementApi, LimitsUnitOfWorkExecutor limitsUnitOfWorkExecutor) {
        this.userManagementApi = userManagementApi;
        this.limitsUnitOfWorkExecutor = limitsUnitOfWorkExecutor;
    }

    /**
     * This is very very wrong.
     */
    public Flux<LimitsPostResponseBody> createUserLimits(Flux<CreateLimitRequestBody> items) {
        Flux<CreateLimitRequestBody> cleanItems =
            items.map(
                item -> {
                    if (isUUID(item.getUserBBID())) {
                        userManagementApi
                            .getUserByExternalId(item.getUserBBID(), true)
                            .subscribe(userItem -> item.setUserBBID(userItem.getId()));
                    }
                    return item;
                });
        Flux<UnitOfWork<LimitsTask>> unitOfWorkFlux =
            limitsUnitOfWorkExecutor.prepareUnitOfWork(cleanItems);
        return unitOfWorkFlux
            .flatMap(limitsUnitOfWorkExecutor::executeUnitOfWork)
            .flatMap(
                limitsTaskUnitOfWork -> {
                    Stream<LimitsPostResponseBody> stream =
                        limitsTaskUnitOfWork.getStreamTasks().stream().map(LimitsTask::getResponse);
                    return Flux.fromStream(stream);
                });
    }

    private boolean isUUID(String input) {
        Pattern pattern =
            Pattern.compile(
                "/^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/");
        return pattern.matcher(input).matches();
    }
}
