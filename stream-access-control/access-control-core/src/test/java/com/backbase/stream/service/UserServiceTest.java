package com.backbase.stream.service;

import static com.backbase.stream.test.LambdaAssertions.assertEqualsTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.backbase.dbs.user.api.service.v2.IdentityManagementApi;
import com.backbase.dbs.user.api.service.v2.UserManagementApi;
import com.backbase.identity.integration.api.service.v1.RealmApi;
import com.backbase.identity.integration.api.service.v1.model.EnhancedUserRepresentation;
import com.backbase.stream.legalentity.model.User;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private UserService subject;

    @Mock
    private UserManagementApi usersApi;

    @Mock
    private IdentityManagementApi identityManagementApi;

    @Mock
    private RealmApi realmApi;

    @BeforeEach
    void setup() {
        subject = new UserService(usersApi, identityManagementApi, Optional.of(realmApi));
    }

    @Test
    void lockUser() {
        String internalId = "someInternalId";
        String realm = "someRealm";
        String email = "some@email.com";

        EnhancedUserRepresentation eur = new EnhancedUserRepresentation().id(internalId).email(email);
        when(realmApi.getUser(internalId, realm)).thenReturn(Mono.just(eur));
        when(realmApi.putUser(eq(internalId), eq(realm), any())).thenReturn(Mono.empty().then());


        User user = new User().internalId(internalId);
        Mono<User> result = subject.lockUser(user, realm);


        result.subscribe(assertEqualsTo(user));
    }
}