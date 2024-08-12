package com.backbase.streams.tailoredvalue.plan;

import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.exception.StreamTaskException;
import com.backbase.streams.tailoredvalue.configuration.PlansProperties;
import com.backbase.tailoredvalue.planmanager.service.api.v0.PlansApi;
import com.backbase.tailoredvalue.planmanager.service.api.v0.UserPlansApi;
import com.backbase.tailoredvalue.planmanager.service.api.v0.model.PlansGetResponseBody;
import com.backbase.tailoredvalue.planmanager.service.api.v0.model.UserPlanUpdateRequestBody;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class PlansSaga implements StreamTaskExecutor<PlansTask> {

    private final PlansApi plansApi;
    private final UserPlansApi userPlansApi;
    private final PlansProperties plansProperties;

    // Map of plan-name to plan-id
    private Map<String, String> plansMap = new HashMap<>();

    @PostConstruct
    public void init() {
        if (!isEnabled()) {
            log.info("PlanSaga is disabled");
            return;
        }
        log.info("PostConstruct init to populate all plans with their ids");

        plansApi.getPlans(new HashSet<>(), new HashSet<>(), "")
                .doOnNext(this::processPlans).subscribe();

        if (plansMap.isEmpty()) {
            throw new RuntimeException("Error getting all plans");
        }
    }

    public boolean isEnabled() {
        if (plansProperties == null) {
            return false;
        }
        return plansProperties.isEnabled();
    }

    @Override
    public Mono<PlansTask> executeTask(PlansTask plansTask) {
        UserPlanUpdateRequestBody userPlanUpdateRequestBody = plansTask.getReqData();

        if (!plansMap.containsKey(plansTask.getPlanName())) {
            log.warn("Plan {} not found", plansTask.getPlanName());
            return Mono.just(plansTask);
        }
        // Set planId
        userPlanUpdateRequestBody.setId(plansMap.get(plansTask.getPlanName()));
        log.info("Started ingestion of plans {} for user {}",
                userPlanUpdateRequestBody.getId(), plansTask.getInternalUserId());
        return userPlansApi.updateUserPlan(plansTask.getInternalUserId(), userPlanUpdateRequestBody)
                .map(userPlanUpdateResponseBody -> {
                    log.info("Plan updated: {}", userPlanUpdateResponseBody.toString());
                    return plansTask;
                })
                .onErrorResume(throwable -> {
                    log.error("Error updating plan", throwable);
                    return Mono.error(new StreamTaskException(plansTask, throwable, "Error updating plan"));
                });
    }

    @Override
    public Mono<PlansTask> rollBack(PlansTask streamTask) {
        return null;
    }

    private void processPlans(PlansGetResponseBody responseBody) {
        responseBody.getPlans().forEach(plan -> plansMap.put(plan.getName(), plan.getId()));
    }

    public Map<String, String> getPlansMap() {
        return plansMap;
    }
}
