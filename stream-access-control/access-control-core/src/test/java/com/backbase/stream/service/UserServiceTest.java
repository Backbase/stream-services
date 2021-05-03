package com.backbase.stream.service;

import static com.backbase.stream.test.LambdaAssertions.assertEqualsTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.dbs.user.api.service.v2.IdentityManagementApi;
import com.backbase.dbs.user.api.service.v2.UserManagementApi;
import com.backbase.identity.integration.api.service.v1.IdentityIntegrationServiceApi;
import com.backbase.identity.integration.api.service.v1.model.EnhancedUserRepresentation;
import com.backbase.identity.integration.api.service.v1.model.UserRequestBody;
import com.backbase.stream.legalentity.model.User;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private UserService subject;

    @Mock
    private UserManagementApi usersApi;

    @Mock
    private IdentityManagementApi identityManagementApi;

    @Mock
    private IdentityIntegrationServiceApi identityIntegrationApi;

    @BeforeEach
    void setup() {
        subject = new UserService(usersApi, identityManagementApi, Optional.of(identityIntegrationApi));
    }

    @Test
    void changeEnableStatusToDisable() {
        String internalId = "someInternalId";
        String realm = "someRealm";
        String email = "some@email.com";

        EnhancedUserRepresentation eur = new EnhancedUserRepresentation().id(internalId).email(email).enabled(true);
        when(identityIntegrationApi.getUserById(realm, internalId)).thenReturn(Mono.just(eur));
        when(identityIntegrationApi.updateUserById(eq(realm), eq(internalId), any())).thenReturn(Mono.empty().then());


        User user = new User().internalId(internalId).locked(true);
        Mono<User> result = subject.changeEnableStatus(user, realm);


        result.subscribe(assertEqualsTo(user));
        verify(identityIntegrationApi).getUserById(eq(realm), eq(internalId));
        UserRequestBody expectedUser = new UserRequestBody().id(internalId).email(email).enabled(false)
            .credentials(Collections.emptyList());
        verify(identityIntegrationApi).updateUserById(eq(realm), eq(internalId), eq(expectedUser));
    }

    @Test
    void changeEnableStatusToEnabled() {
        String internalId = "someInternalId";
        String realm = "someRealm";
        String email = "some@email.com";

        EnhancedUserRepresentation eur = new EnhancedUserRepresentation().id(internalId).email(email).enabled(false);
        when(identityIntegrationApi.getUserById(realm, internalId)).thenReturn(Mono.just(eur));
        when(identityIntegrationApi.updateUserById(eq(realm), eq(internalId), any())).thenReturn(Mono.empty().then());


        User user = new User().internalId(internalId).locked(false);
        Mono<User> result = subject.changeEnableStatus(user, realm);


        result.subscribe(assertEqualsTo(user));
        verify(identityIntegrationApi).getUserById(eq(realm), eq(internalId));
        UserRequestBody expectedUser = new UserRequestBody().id(internalId).email(email).enabled(true)
            .credentials(Collections.emptyList());
        verify(identityIntegrationApi).updateUserById(eq(realm), eq(internalId), eq(expectedUser));
    }
}