package com.backbase.streams.tailoredvalue.plan;

import com.backbase.stream.worker.exception.StreamTaskException;
import com.backbase.streams.tailoredvalue.configuration.PlansProperties;
import com.backbase.tailoredvalue.planmanager.service.api.v0.PlansApi;
import com.backbase.tailoredvalue.planmanager.service.api.v0.UserPlansApi;
import com.backbase.tailoredvalue.planmanager.service.api.v0.model.Plan;
import com.backbase.tailoredvalue.planmanager.service.api.v0.model.PlansGetResponseBody;
import com.backbase.tailoredvalue.planmanager.service.api.v0.model.UserPlanUpdateRequestBody;
import com.backbase.tailoredvalue.planmanager.service.api.v0.model.UserPlanUpdateResponseBody;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PlanSagaTest {

    private static final String PLAN_NAME = "plan-name";
    private static final String INTERNAl_USER_ID = "interUserId";
    private static final String PLAN_ID = "plan-id";

    @InjectMocks
    private PlansSaga plansSaga;

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

        //When Postcontruct method runs
        plansSaga.init();

        //Then verify plansAPI gets called
        verify(plansApi).getPlans(any(), any(), any());
        // Verify that the plansMap get populated with size 1 and that the planName is the key and planId is the value
        assertEquals(1, plansSaga.getPlansMap().size());
        assertEquals(PLAN_ID, plansSaga.getPlansMap().get(PLAN_NAME));

    }

    @Test
    void testPostConstruct_whenPlansPropertiesIsNotEnabled() {
        //When Postcontruct method runs
        plansSaga.init();

        //Then verify plansAPI is not called so the plansMap should be empty
        assertEquals(0, plansSaga.getPlansMap().size());


    }

    @Test
    void testExecuteTask_whenPlansPropertiesIsEnabled() {
        //Given
        when(plansProperties.isEnabled()).thenReturn(true);
        mockPlansApi();
        PlansTask plansTask = createPlanTask();
        when(userPlansApi.updateUserPlan(plansTask.getInternalUserId(), plansTask.getReqData())).thenReturn(Mono.just(new UserPlanUpdateResponseBody()));
        plansSaga.init();

        //When
        plansSaga.executeTask(plansTask).block();

        //Then
        // PlanTask ReqData should have the planId set
        assertEquals(PLAN_ID, plansTask.getReqData().getId());
        //Then verify userPlansApi gets called
        verify(userPlansApi).updateUserPlan(plansTask.getInternalUserId(), plansTask.getReqData());
    }

    @Test
    void testExecuteTask_whenPlansPropertiesIsEnabled_andPlansApi_throws_Exception() {
        //Given
        when(plansProperties.isEnabled()).thenReturn(true);
        when(plansApi.getPlans(any(), any(), any())).thenReturn(Mono.error(new WebClientResponseException(400, "Bad Request", null, null, null)));
        //When Then
        Assertions.assertThrows(RuntimeException.class, () -> plansSaga.init());
    }

    @Test
    void testExecuteTask_whenPlansPropertiesIsEnabled_andUserPlansApi_throws_Exception() {
        //Given
        when(plansProperties.isEnabled()).thenReturn(true);
        mockPlansApi();
        PlansTask plansTask = createPlanTask();
        when(userPlansApi.updateUserPlan(plansTask.getInternalUserId(), plansTask.getReqData()))
                .thenReturn(Mono.error(new WebClientResponseException(400, "Bad Request", null, null, null)));
        plansSaga.init();

        //When Then
        Assertions.assertThrows(StreamTaskException.class, () -> plansSaga.executeTask(plansTask).block());
    }


    private void mockPlansApi() {
        PlansGetResponseBody plansGetResponseBody = new PlansGetResponseBody();
        plansGetResponseBody.setPlans(List.of(new Plan().id(PLAN_ID).name(PLAN_NAME)));
        when(plansApi.getPlans(any(), any(), any())).thenAnswer(invocation -> Mono.just(plansGetResponseBody));
    }

    private PlansTask createPlanTask() {
        UserPlanUpdateRequestBody reqBody = new UserPlanUpdateRequestBody();
        reqBody.setId(""); // To be filled by saga itself
        reqBody.serviceAgreementId("serviceAgreementId");
        reqBody.setLegalEntityId("legalEntityId");
        return new PlansTask("work-id", INTERNAl_USER_ID, reqBody, PLAN_NAME);
    }
}
