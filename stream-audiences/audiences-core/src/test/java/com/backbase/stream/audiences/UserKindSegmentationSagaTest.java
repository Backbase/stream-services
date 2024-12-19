package com.backbase.stream.audiences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.audiences.collector.api.service.v1.HandlersServiceApi;
import com.backbase.audiences.collector.api.service.v1.model.CustomerOnboardedRequest;
import com.backbase.audiences.collector.api.service.v1.model.CustomerOnboardedRequest.UserKindEnum;
import com.backbase.stream.configuration.UserKindSegmentationProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class UserKindSegmentationSagaTest {

  @Mock private HandlersServiceApi handlersServiceApi;

  @Mock private UserKindSegmentationProperties userKindSegmentationProperties;

  @InjectMocks private UserKindSegmentationSaga userKindSegmentationSaga;

  @Test
  void testExecuteTask() {
    var task = createTask();
    when(handlersServiceApi.customerOnboarded(any())).thenReturn(Mono.empty());

    userKindSegmentationSaga.executeTask(task).block();

    verify(handlersServiceApi).customerOnboarded(any());
  }

  @Test
  void isDisabledByDefault() {
    var saga = new UserKindSegmentationSaga(handlersServiceApi, null);

    assertFalse(saga.isEnabled());
  }

  @Test
  void defaultCustomerCategoryIsNullWhenSagaIsDisabled() {
    when(userKindSegmentationProperties.enabled()).thenReturn(false);

    assertNull(userKindSegmentationSaga.getDefaultCustomerCategory());
  }

  @Test
  void rollbackReturnsNull() {
    assertNull(userKindSegmentationSaga.rollBack(createTask()));
  }

  @Test
  void returnsDefaultCustomerCategoryFromProperties() {
    when(userKindSegmentationProperties.enabled()).thenReturn(true);
    when(userKindSegmentationProperties.defaultCustomerCategory()).thenReturn("RETAIL");

    assertEquals("RETAIL", userKindSegmentationSaga.getDefaultCustomerCategory());
  }

  private UserKindSegmentationTask createTask() {
    var task = new UserKindSegmentationTask();
    task.setCustomerOnboardedRequest(
        new CustomerOnboardedRequest()
            .internalUserId("internal-id")
            .userKind(UserKindEnum.RETAILCUSTOMER));
    return task;
  }
}
