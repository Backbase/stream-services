package com.backbase.stream.service;

import static com.backbase.stream.legalentity.model.IdentityUserLinkStrategy.CREATE_IN_IDENTITY;
import static com.backbase.stream.legalentity.model.IdentityUserLinkStrategy.IMPORT_FROM_IDENTIY;
import static com.backbase.stream.test.LambdaAssertions.assertEqualsTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.backbase.dbs.user.api.service.v2.IdentityManagementApi;
import com.backbase.dbs.user.api.service.v2.UserManagementApi;
import com.backbase.dbs.user.api.service.v2.model.CreateIdentityRequest;
import com.backbase.dbs.user.api.service.v2.model.CreateIdentityResponse;
import com.backbase.dbs.user.api.service.v2.model.UpdateIdentityRequest;
import com.backbase.identity.integration.api.service.v1.IdentityIntegrationServiceApi;
import com.backbase.identity.integration.api.service.v1.model.EnhancedUserRepresentation;
import com.backbase.identity.integration.api.service.v1.model.UserRequestBody;
import com.backbase.stream.legalentity.model.EmailAddress;
import com.backbase.stream.legalentity.model.IdentityUserLinkStrategy;
import com.backbase.stream.legalentity.model.PhoneNumber;
import com.backbase.stream.legalentity.model.User;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.backbase.stream.product.task.ProductGroupTask;
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

    @Test
    void changeEnableStatusDoesntChangeWhenUndefinedLocked() {
        String internalId = "someInternalId";
        String realm = "someRealm";
        String email = "some@email.com";

        EnhancedUserRepresentation eur = new EnhancedUserRepresentation().id(internalId).email(email).enabled(false);
        when(identityIntegrationApi.getUserById(realm, internalId)).thenReturn(Mono.just(eur));


        User user = new User().internalId(internalId).locked(null);
        Mono<User> result = subject.changeEnableStatus(user, realm);


        result.subscribe(assertEqualsTo(user));
        verify(identityIntegrationApi).getUserById(eq(realm), eq(internalId));
        UserRequestBody expectedUser = new UserRequestBody().id(internalId).email(email).enabled(true)
            .credentials(Collections.emptyList());
        verifyNoMoreInteractions(identityIntegrationApi);
    }

    @Test
    void createOrImportIdentityUserUpdateAttributesWhenIFIStrategy() {
        final String internalId = "someInternalId";
        final String externalId = "someExternalId";
        final String legalEntityId = "someLegalEntityId";
        final Map<String, String> attributesMap = Collections.singletonMap("someKey", "someValue");
        final IdentityUserLinkStrategy strategy = IMPORT_FROM_IDENTIY;

        CreateIdentityResponse response = new CreateIdentityResponse().externalId(externalId).internalId(internalId);
        when(identityManagementApi.createIdentity(any())).thenReturn(Mono.just(response));

        when(identityManagementApi.updateIdentity(eq(internalId), any())).thenReturn(Mono.empty().then());

        User user = new User().externalId(externalId).attributes(attributesMap)
            .identityLinkStrategy(strategy);


        Mono<User> result = subject.createOrImportIdentityUser(user, legalEntityId, new ProductGroupTask());


        result.subscribe(assertEqualsTo(user));
        CreateIdentityRequest expectedCreateIdentityRequest = new CreateIdentityRequest().externalId(externalId)
            .legalEntityInternalId(legalEntityId);
        verify(identityManagementApi).createIdentity(expectedCreateIdentityRequest);
        UpdateIdentityRequest expectedUpdateIdentityRequest = new UpdateIdentityRequest().attributes(attributesMap);
        verify(identityManagementApi).updateIdentity(internalId, expectedUpdateIdentityRequest);
    }

    @Test
    void createOrImportIdentityUserDontUpdateWhenNoAttributesPresent() {
        final String internalId = "someInternalId";
        final String externalId = "someExternalId";
        final String legalEntityId = "someLegalEntityId";
        final IdentityUserLinkStrategy strategy = IMPORT_FROM_IDENTIY;

        CreateIdentityResponse response = new CreateIdentityResponse().externalId(externalId).internalId(internalId);
        when(identityManagementApi.createIdentity(any())).thenReturn(Mono.just(response));

        User user = new User().externalId(externalId).identityLinkStrategy(strategy);


        Mono<User> result = subject.createOrImportIdentityUser(user, legalEntityId, new ProductGroupTask());


        result.subscribe(assertEqualsTo(user));
        CreateIdentityRequest expectedCreateIdentityRequest = new CreateIdentityRequest().externalId(externalId)
            .legalEntityInternalId(legalEntityId);
        verify(identityManagementApi).createIdentity(expectedCreateIdentityRequest);
        verifyNoMoreInteractions(identityManagementApi);
    }

    @Test
    void createOrImportIdentityUserDoesNotUpdateUserWhenNoIFIStrategy() {
        final String internalId = "someInternalId";
        final String externalId = "someExternalId";
        final String legalEntityId = "someLegalEntityId";
        final Map<String, String> attributesMap = Collections.singletonMap("someKey", "someValue");
        final IdentityUserLinkStrategy strategy = CREATE_IN_IDENTITY;
        final String emailAddress = "some@email.com";
        final String mobileNumber = "123456";
        final String fullName = "someName";

        CreateIdentityResponse response = new CreateIdentityResponse().externalId(externalId).internalId(internalId);
        when(identityManagementApi.createIdentity(any())).thenReturn(Mono.just(response));

        User user = new User().externalId(externalId).attributes(attributesMap)
            .identityLinkStrategy(strategy).fullName(fullName)
            .emailAddress(new EmailAddress().address(emailAddress))
            .mobileNumber(new PhoneNumber().number(mobileNumber));


        Mono<User> result = subject.createOrImportIdentityUser(user, legalEntityId, new ProductGroupTask());


        result.subscribe(assertEqualsTo(user));
        CreateIdentityRequest expectedCreateIdentityRequest = new CreateIdentityRequest().externalId(externalId)
            .legalEntityInternalId(legalEntityId).emailAddress(emailAddress).mobileNumber(mobileNumber)
            .fullName(fullName).attributes(attributesMap);
        verify(identityManagementApi).createIdentity(expectedCreateIdentityRequest);
        verifyNoMoreInteractions(identityManagementApi);
    }
}
