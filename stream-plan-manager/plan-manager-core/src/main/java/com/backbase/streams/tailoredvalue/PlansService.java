package com.backbase.streams.tailoredvalue;

import com.backbase.streams.tailoredvalue.configuration.PlansProperties;
import com.backbase.streams.tailoredvalue.exceptions.PlanManagerException;
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
public class PlansService {

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

        plansApi.getPlans(new HashSet<>(), new HashSet<>(), null)
                .doOnNext(this::processPlans)
                .doOnError(error -> {
                    throw new PlanManagerException(error, "Error getting all plans");
                }).subscribe();
    }

    public boolean isEnabled() {
        if (plansProperties == null) {
            return false;
        }
        return plansProperties.isEnabled();
    }

    public Mono<Void> updateUserPlan(String internalUserId, UserPlanUpdateRequestBody userPlanUpdateRequestBody, String planName) {

        if (!plansMap.containsKey(planName)) {
            log.warn("No PlanId found for planName = " + planName);
            return Mono.error(new PlanManagerException("No PlanId found for planName = " + planName));
        }
        // Set planId
        userPlanUpdateRequestBody.setId(plansMap.get(planName));
        log.info("Started ingestion of plans {} for user {}",
                userPlanUpdateRequestBody.getId(), internalUserId);
        return userPlansApi.updateUserPlan(internalUserId, userPlanUpdateRequestBody)
                .map(userPlanUpdateResponseBody -> {
                    log.info("Plan updated: {}", userPlanUpdateResponseBody.toString());
                    return Mono.empty();
                })
                .onErrorResume(throwable -> {
                    log.error("Error updating plan", throwable);
                    return Mono.error(new PlanManagerException(throwable, "Error updating plan"));
                }).then();
    }

    private void processPlans(PlansGetResponseBody responseBody) {
        if (responseBody.getPlans().isEmpty()) {
            throw new PlanManagerException("Received plans list is empty");
        }
        responseBody.getPlans().forEach(plan -> plansMap.put(plan.getName(), plan.getId()));
    }

    public Map<String, String> getPlansMap() {
        return plansMap;
    }
}
