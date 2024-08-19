package com.backbase.streams.tailoredvalue.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.streams.tailoredvalue.PlansService;
import com.backbase.streams.tailoredvalue.configuration.PlansProperties;
import com.backbase.streams.tailoredvalue.exceptions.PlanManagerException;
import com.backbase.tailoredvalue.planmanager.service.api.v0.PlansApi;
import com.backbase.tailoredvalue.planmanager.service.api.v0.UserPlansApi;
import com.backbase.tailoredvalue.planmanager.service.api.v0.model.Plan;
import com.backbase.tailoredvalue.planmanager.service.api.v0.model.PlansGetResponseBody;
import com.backbase.tailoredvalue.planmanager.service.api.v0.model.UserPlanUpdateRequestBody;
import com.backbase.tailoredvalue.planmanager.service.api.v0.model.UserPlanUpdateResponseBody;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    private static final String PLAN_NAME = "plan-name";
    private static final String INTERNAL_USER_ID = "interUserId";
    private static final String PLAN_ID = "plan-id";

    @InjectMocks
    private PlansService plansService;

    @Mock
    PlansApi plansApi;

    @Mock
    UserPlansApi userPlansApi;

    @Mock
    PlansProperties plansProperties;

    @Test
    void testPostConstruct_whenPlansPropertiesIsEnabled() {
        //Given
        when(plansProperties.isEnabled()).thenReturn(true);
        mockPlansApi();

        //When Post construct method runs
        plansService.init();

        //Then verify plansAPI gets called
        verify(plansApi).getPlans(any(), any(), any());
        // Verify that the plansMap get populated with size 1 and that the planName is the key and planId is the value
        assertEquals(1, plansService.getPlansMap().size());
        assertEquals(PLAN_ID, plansService.getPlansMap().get(PLAN_NAME));

    }

    @Test
    void testPostConstruct_whenPlansPropertiesIsNotEnabled() {
        //When Post construct method runs
        plansService.init();

        //Then verify plansAPI is not called so the plansMap should be empty
        assertEquals(0, plansService.getPlansMap().size());


    }

    @Test
    void testExecuteTask_whenPlansPropertiesIsEnabled() {
        //Given
        when(plansProperties.isEnabled()).thenReturn(true);
        mockPlansApi();
        UserPlanUpdateRequestBody reqBody = createUserPlanUpdateRequestBody();
        when(userPlansApi.updateUserPlan(INTERNAL_USER_ID, reqBody))
                .thenReturn(Mono.just(new UserPlanUpdateResponseBody()));
        plansService.init();

        //When
        plansService.updateUserPlan(INTERNAL_USER_ID, reqBody, PLAN_NAME).block();

        //Then
        // PlanTask ReqData should have the planId set
        assertEquals(PLAN_ID, reqBody.getId());
        //Then verify userPlansApi gets called
        verify(userPlansApi).updateUserPlan(INTERNAL_USER_ID, reqBody);
    }

    @Test
    void testExecuteTask_whenPlansPropertiesIsEnabled_andUserPlansApi_throws_Exception() {
        //Given
        when(plansProperties.isEnabled()).thenReturn(true);
        mockPlansApi();
        UserPlanUpdateRequestBody reqBody = createUserPlanUpdateRequestBody();
        when(userPlansApi.updateUserPlan(INTERNAL_USER_ID, reqBody))
                .thenReturn(Mono.error(new WebClientResponseException(400, "Bad Request", null, null, null)));
        plansService.init();

        //When
        Mono<Void> mono = plansService.updateUserPlan(INTERNAL_USER_ID, reqBody, PLAN_NAME);

        //Then
        Assertions.assertThrows(PlanManagerException.class,mono::block);
    }


    private void mockPlansApi() {
        PlansGetResponseBody plansGetResponseBody = new PlansGetResponseBody();
        plansGetResponseBody.setPlans(List.of(new Plan().id(PLAN_ID).name(PLAN_NAME)));
        when(plansApi.getPlans(any(), any(), any())).thenAnswer(invocation -> Mono.just(plansGetResponseBody));
    }

    private UserPlanUpdateRequestBody createUserPlanUpdateRequestBody() {
        UserPlanUpdateRequestBody reqBody = new UserPlanUpdateRequestBody();
        reqBody.setId(""); // To be filled by plansService itself
        reqBody.serviceAgreementId("serviceAgreementId");
        reqBody.setLegalEntityId("legalEntityId");
        return reqBody;
    }
}
