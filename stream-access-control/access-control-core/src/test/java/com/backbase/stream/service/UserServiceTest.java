package com.backbase.stream.service;

import static com.backbase.stream.LambdaAssertions.assertEqualsTo;
import static com.backbase.stream.legalentity.model.IdentityUserLinkStrategy.CREATE_IN_IDENTITY;
import static com.backbase.stream.legalentity.model.IdentityUserLinkStrategy.IMPORT_FROM_IDENTIY;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.backbase.dbs.user.api.service.v2.IdentityManagementApi;
import com.backbase.dbs.user.api.service.v2.UserManagementApi;
import com.backbase.dbs.user.api.service.v2.model.*;
import com.backbase.identity.integration.api.service.v1.IdentityIntegrationServiceApi;
import com.backbase.identity.integration.api.service.v1.model.EnhancedUserRepresentation;
import com.backbase.identity.integration.api.service.v1.model.UserRequestBody;
import com.backbase.stream.legalentity.model.EmailAddress;
import com.backbase.stream.legalentity.model.IdentityUserLinkStrategy;
import com.backbase.stream.legalentity.model.PhoneNumber;
import com.backbase.stream.legalentity.model.User;
import com.backbase.stream.product.task.ProductGroupTask;
import com.backbase.stream.worker.model.StreamTask;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private UserService subject;

    @Mock private UserManagementApi usersApi;

    @Mock private IdentityManagementApi identityManagementApi;

    @Mock private IdentityIntegrationServiceApi identityIntegrationApi;

    @BeforeEach
    void setup() {
        subject =
                new UserService(
                        usersApi, identityManagementApi, Optional.of(identityIntegrationApi));
    }

    @Test
    void updateUserStateSetStatusToDisable() {
        String internalId = "someInternalId";
        String realm = "someRealm";
        String email = "some@email.com";

        EnhancedUserRepresentation eur =
                new EnhancedUserRepresentation().id(internalId).email(email).enabled(true);
        when(identityIntegrationApi.getUserById(realm, internalId)).thenReturn(Mono.just(eur));
        when(identityIntegrationApi.updateUserById(eq(realm), eq(internalId), any()))
                .thenReturn(Mono.empty().then());

        User user = new User().internalId(internalId).locked(true);
        Mono<User> result = subject.updateUserState(user, realm);

        result.subscribe(assertEqualsTo(user));
        verify(identityIntegrationApi).getUserById(eq(realm), eq(internalId));
        UserRequestBody expectedUser =
                new UserRequestBody()
                        .id(internalId)
                        .email(email)
                        .enabled(false)
                        .credentials(Collections.emptyList());
        verify(identityIntegrationApi).updateUserById(eq(realm), eq(internalId), eq(expectedUser));
    }

    @Test
    void updateUserStateSetStatusToEnabled() {
        String internalId = "someInternalId";
        String realm = "someRealm";
        String email = "some@email.com";

        EnhancedUserRepresentation eur =
                new EnhancedUserRepresentation().id(internalId).email(email).enabled(false);
        when(identityIntegrationApi.getUserById(realm, internalId)).thenReturn(Mono.just(eur));
        when(identityIntegrationApi.updateUserById(eq(realm), eq(internalId), any()))
                .thenReturn(Mono.empty().then());

        User user = new User().internalId(internalId).locked(false);
        Mono<User> result = subject.updateUserState(user, realm);

        result.subscribe(assertEqualsTo(user));
        verify(identityIntegrationApi).getUserById(eq(realm), eq(internalId));
        UserRequestBody expectedUser =
                new UserRequestBody()
                        .id(internalId)
                        .email(email)
                        .enabled(true)
                        .credentials(Collections.emptyList());
        verify(identityIntegrationApi).updateUserById(eq(realm), eq(internalId), eq(expectedUser));
    }

    @Test
    void updateUserStateStatusDoesntChangeWhenUndefinedLocked() {
        String internalId = "someInternalId";
        String realm = "someRealm";
        String email = "some@email.com";

        EnhancedUserRepresentation eur =
                new EnhancedUserRepresentation().id(internalId).email(email).enabled(false);
        when(identityIntegrationApi.getUserById(realm, internalId)).thenReturn(Mono.just(eur));

        User user = new User().internalId(internalId).locked(null);
        Mono<User> result = subject.updateUserState(user, realm);

        result.subscribe(assertEqualsTo(user));
        verify(identityIntegrationApi).getUserById(eq(realm), eq(internalId));
        verifyNoMoreInteractions(identityIntegrationApi);
    }

    @Test
    void updateUserStateAdditionalRealmRoles() {
        String internalId = "someInternalId";
        String realm = "someRealm";
        String email = "some@email.com";

        EnhancedUserRepresentation enhancedUserRepresentation =
                new EnhancedUserRepresentation()
                        .id(internalId)
                        .email(email)
                        .addRealmRolesItem("banking");
        when(identityIntegrationApi.getUserById(realm, internalId))
                .thenReturn(Mono.just(enhancedUserRepresentation));
        when(identityIntegrationApi.updateUserById(
                        eq(realm), eq(internalId), any(UserRequestBody.class)))
                .thenReturn(Mono.empty());

        User user =
                new User()
                        .internalId(internalId)
                        .additionalRealmRoles(Arrays.asList("private", "private", "wealth"));
        Mono<User> result = subject.updateUserState(user, realm);

        result.subscribe(assertEqualsTo(user));
        verify(identityIntegrationApi).getUserById(realm, internalId);
        UserRequestBody expectedUser =
                new UserRequestBody()
                        .id(internalId)
                        .email(email)
                        .credentials(Collections.emptyList())
                        .realmRoles(Arrays.asList("banking", "private", "wealth"));
        verify(identityIntegrationApi).updateUserById(realm, internalId, expectedUser);
    }

    @Test
    void assignAdditionalGroups() {
        String internalId = "someInternalId";
        String realm = "someRealm";
        String email = "some@email.com";

        EnhancedUserRepresentation enhancedUserRepresentation =
                new EnhancedUserRepresentation()
                        .id(internalId)
                        .email(email)
                        .addGroupsItem("group1")
                        .addGroupsItem("group2");
        when(identityIntegrationApi.getUserById(realm, internalId))
                .thenReturn(Mono.just(enhancedUserRepresentation));
        when(identityIntegrationApi.updateUserById(
                        eq(realm), eq(internalId), any(UserRequestBody.class)))
                .thenReturn(Mono.empty());

        User user =
                new User()
                        .internalId(internalId)
                        .additionalGroups(Arrays.asList("group2", "group3"));
        Mono<User> result = subject.updateUserState(user, realm);

        result.subscribe(assertEqualsTo(user));
        verify(identityIntegrationApi).getUserById(realm, internalId);
        UserRequestBody expectedUser =
                new UserRequestBody()
                        .id(internalId)
                        .email(email)
                        .credentials(Collections.emptyList())
                        .groups(Arrays.asList("group1", "group2", "group3"));
        verify(identityIntegrationApi).updateUserById(realm, internalId, expectedUser);
    }

    @Test
    void createOrImportIdentityUserUpdateAttributesWhenIFIStrategy() {
        final String internalId = "someInternalId";
        final String externalId = "someExternalId";
        final String legalEntityId = "someLegalEntityId";
        final Map<String, String> attributesMap = Collections.singletonMap("someKey", "someValue");
        final IdentityUserLinkStrategy strategy = IMPORT_FROM_IDENTIY;

        CreateIdentityResponse response =
                new CreateIdentityResponse().externalId(externalId).internalId(internalId);
        when(identityManagementApi.createIdentity(any())).thenReturn(Mono.just(response));

        when(identityManagementApi.updateIdentity(eq(internalId), any()))
                .thenReturn(Mono.empty().then());

        User user =
                new User()
                        .externalId(externalId)
                        .attributes(attributesMap)
                        .identityLinkStrategy(strategy);

        Mono<User> result =
                subject.createOrImportIdentityUser(user, legalEntityId, new ProductGroupTask());

        result.subscribe(assertEqualsTo(user));
        CreateIdentityRequest expectedCreateIdentityRequest =
                new CreateIdentityRequest()
                        .externalId(externalId)
                        .legalEntityInternalId(legalEntityId);
        verify(identityManagementApi).createIdentity(expectedCreateIdentityRequest);
        UpdateIdentityRequest expectedUpdateIdentityRequest =
                new UpdateIdentityRequest().attributes(attributesMap);
        verify(identityManagementApi).updateIdentity(internalId, expectedUpdateIdentityRequest);
    }

    @Test
    void createOrImportIdentityUserDontUpdateWhenNoAttributesPresent() {
        final String internalId = "someInternalId";
        final String externalId = "someExternalId";
        final String legalEntityId = "someLegalEntityId";
        final IdentityUserLinkStrategy strategy = IMPORT_FROM_IDENTIY;

        CreateIdentityResponse response =
                new CreateIdentityResponse().externalId(externalId).internalId(internalId);
        when(identityManagementApi.createIdentity(any())).thenReturn(Mono.just(response));

        User user = new User().externalId(externalId).identityLinkStrategy(strategy);

        Mono<User> result =
                subject.createOrImportIdentityUser(user, legalEntityId, new ProductGroupTask());

        result.subscribe(assertEqualsTo(user));
        CreateIdentityRequest expectedCreateIdentityRequest =
                new CreateIdentityRequest()
                        .externalId(externalId)
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

        CreateIdentityResponse response =
                new CreateIdentityResponse().externalId(externalId).internalId(internalId);
        when(identityManagementApi.createIdentity(any())).thenReturn(Mono.just(response));

        User user =
                new User()
                        .externalId(externalId)
                        .attributes(attributesMap)
                        .identityLinkStrategy(strategy)
                        .fullName(fullName)
                        .emailAddress(new EmailAddress().address(emailAddress))
                        .mobileNumber(new PhoneNumber().number(mobileNumber));

        Mono<User> result =
                subject.createOrImportIdentityUser(user, legalEntityId, new ProductGroupTask());

        result.subscribe(assertEqualsTo(user));
        CreateIdentityRequest expectedCreateIdentityRequest =
                new CreateIdentityRequest()
                        .externalId(externalId)
                        .legalEntityInternalId(legalEntityId)
                        .emailAddress(emailAddress)
                        .mobileNumber(mobileNumber)
                        .fullName(fullName)
                        .attributes(attributesMap);
        verify(identityManagementApi).createIdentity(expectedCreateIdentityRequest);
        verifyNoMoreInteractions(identityManagementApi);
    }

    @Test
    void createOrImportIdentityUserFailure() {
        final String internalId = "someInternalId";
        final String externalId = "someExternalId";
        final String legalEntityId = "someLegalEntityId";
        final Map<String, String> attributesMap = Collections.singletonMap("someKey", "someValue");
        final IdentityUserLinkStrategy strategy = CREATE_IN_IDENTITY;
        final String emailAddress = "some@email.com";
        final String mobileNumber = "123456";
        final String fullName = "someName";

        when(identityManagementApi.createIdentity(any()))
                .thenReturn(
                        Mono.error(
                                WebClientResponseException.create(
                                        500,
                                        "",
                                        new HttpHeaders(),
                                        "Error response".getBytes(StandardCharsets.UTF_8),
                                        null)));

        User user =
                new User()
                        .externalId(externalId)
                        .attributes(attributesMap)
                        .identityLinkStrategy(strategy)
                        .fullName(fullName)
                        .emailAddress(new EmailAddress().address(emailAddress))
                        .mobileNumber(new PhoneNumber().number(mobileNumber));

        Mono<User> result =
                subject.createOrImportIdentityUser(user, legalEntityId, new ProductGroupTask());

        StepVerifier.create(result).expectError().verify();
    }

    @Test
    void getUserByExternalId() {
        final String externalId = "someExternalId";
        final String fullName = "someName";
        GetUser getUser = new GetUser().externalId(externalId).fullName(fullName);

        when(usersApi.getUserByExternalId(externalId, true)).thenReturn(Mono.just(getUser));

        Mono<User> userByExternalId = subject.getUserByExternalId(externalId);
        StepVerifier.create(userByExternalId)
                .assertNext(Assertions::assertNotNull)
                .verifyComplete();
    }

    @Test
    void getUserByExternalIdNotFound() {
        final String externalId = "someExternalId";
        final String fullName = "someName";
        when(usersApi.getUserByExternalId(externalId, true))
                .thenReturn(
                        Mono.error(
                                WebClientResponseException.NotFound.create(
                                        404, "not found", new HttpHeaders(), new byte[0], null)));

        Mono<User> userByExternalId = subject.getUserByExternalId(externalId);
        StepVerifier.create(userByExternalId).expectNextCount(0).verifyComplete();
    }

    @Test
    void createUser() {
        final String internalId = "someInternalId";
        final String externalId = "someExternalId";
        final String legalEntityId = "someLegalEntityId";
        final Map<String, String> attributesMap = Collections.singletonMap("someKey", "someValue");
        final IdentityUserLinkStrategy strategy = CREATE_IN_IDENTITY;
        final String emailAddress = "some@email.com";
        final String mobileNumber = "123456";
        final String fullName = "someName";
        UserExternal userExternal = new UserExternal().externalId(externalId).fullName(fullName);

        when(usersApi.createUser(any())).thenReturn(Mono.just(new UserCreated().id(internalId)));

        User user =
                new User()
                        .externalId(externalId)
                        .attributes(attributesMap)
                        .identityLinkStrategy(strategy)
                        .fullName(fullName)
                        .emailAddress(new EmailAddress().address(emailAddress))
                        .mobileNumber(new PhoneNumber().number(mobileNumber));

        Mono<User> user1 =
                subject.createUser(
                        user,
                        "12345",
                        new StreamTask() {
                            @Override
                            public String getName() {
                                return null;
                            }
                        });

        StepVerifier.create(user1).expectNextCount(1).verifyComplete();
    }

    @Test
    void updateUser() {
        final String externalId = "someExternalId";
        final String fullName = "newName";
        GetUser getUser = new GetUser().externalId(externalId).fullName(fullName);

        BatchResponseItem batchResponseItem =
                new BatchResponseItem().status(BatchResponseItem.StatusEnum._200);

        when(usersApi.updateUserInBatch(any())).thenReturn(Flux.just(batchResponseItem));
        when(usersApi.getUserByExternalId(externalId, true)).thenReturn(Mono.just(getUser));

        User user = new User().externalId(externalId).fullName("oldName");
        Mono<User> result = subject.updateUser(user);

        User expectedUser = new User().externalId(externalId).fullName(fullName);

        StepVerifier.create(result).assertNext(assertEqualsTo(expectedUser)).verifyComplete();
    }

    @Test
    void updateUser_fail() {
        final String externalId = "someExternalId";
        final String fullName = "newName";
        GetUser getUser = new GetUser().externalId(externalId).fullName(fullName);

        when(usersApi.updateUserInBatch(any()))
                .thenReturn(
                        Flux.error(
                                WebClientResponseException.create(
                                        500,
                                        "",
                                        new HttpHeaders(),
                                        "Error response".getBytes(StandardCharsets.UTF_8),
                                        null)));
        when(usersApi.getUserByExternalId(externalId, true)).thenReturn(Mono.just(getUser));

        User user = new User().externalId(externalId).fullName("oldName");
        Mono<User> result = subject.updateUser(user);

        StepVerifier.create(result).expectError().verify();
    }

    @Test
    void updateUser_batchResponseItem_fail() {
        final String externalId = "someExternalId";
        final String fullName = "newName";
        GetUser getUser = new GetUser().externalId(externalId).fullName(fullName);

        BatchResponseItem batchResponseItem =
                new BatchResponseItem().status(BatchResponseItem.StatusEnum._400);

        when(usersApi.updateUserInBatch(any())).thenReturn(Flux.just(batchResponseItem));
        when(usersApi.getUserByExternalId(externalId, true)).thenReturn(Mono.just(getUser));

        User user = new User().externalId(externalId).fullName("oldName");
        Mono<User> result = subject.updateUser(user);

        StepVerifier.create(result).expectError().verify();
    }
}
