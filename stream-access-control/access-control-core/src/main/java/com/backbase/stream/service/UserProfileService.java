package com.backbase.stream.service;

import com.backbase.dbs.userprofile.api.UserProfileApi;
import com.backbase.dbs.userprofile.model.CreateUserProfile;
import com.backbase.dbs.userprofile.model.GetUserProfile;
import com.backbase.dbs.userprofile.model.ReplaceUserProfile;
import com.backbase.stream.legalentity.model.User;
import com.backbase.stream.mapper.UserProfileMapper;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Stream User Profile Management
 */
@Slf4j
@AllArgsConstructor
public class UserProfileService {

    @NonNull
    private final UserProfileApi userProfileApi;

    private final UserProfileMapper mapper = Mappers.getMapper(UserProfileMapper.class);

    public Mono<GetUserProfile> upsertUserProfile(CreateUserProfile requestBody) {

        Mono<GetUserProfile> getExistingUser = getUserProfileByUserID(requestBody.getUserId())
            .flatMap(getUserProfile -> updateUserProfile(getUserProfile.getUserId(), mapper.toUpdate(requestBody)))
            .doOnNext(
                existingUser -> log.info("User Profile updated for User with ID: {}", existingUser.getExternalId()));
        Mono<GetUserProfile> createNewUser = createUserProfile(requestBody)
            .doOnNext(createdUser -> log.info("User Profile created for User with ID: {}", createdUser.getExternalId()));
        return getExistingUser.switchIfEmpty(createNewUser);
    }

    public Mono<GetUserProfile> createUserProfile(CreateUserProfile requestBody) {
        return userProfileApi.postUserProfile(requestBody)
            .doOnError(WebClientResponseException.class, throwable ->
                log.error("Failed to create User Profile: {}\n{}", requestBody.getExternalId(),
                    throwable.getResponseBodyAsString()));
    }

    public Mono<GetUserProfile> updateUserProfile(String userId, ReplaceUserProfile requestBody) {
        return userProfileApi.putUserProfile(userId, requestBody)
            .doOnError(WebClientResponseException.class, throwable ->
                log.error("Failed to update User Profile: {}\n{}", requestBody.getExternalId(),
                    throwable.getResponseBodyAsString()));
    }

    public Mono<Void> deleteUserProfile(String userId) {
        return userProfileApi.deleteUserProfile(userId)
            .doOnError(WebClientResponseException.class, throwable ->
                log.error("Failed to delete User Profile: {}\n{}", userId, throwable.getResponseBodyAsString()));
    }

    public Mono<GetUserProfile> getUserProfileByUserID(String userId) {
        if (userId == null) {
            return Mono.empty();
        }
        return userProfileApi.getUserProfile(userId)
            .doOnNext(
                userProfileItem -> log.info("Found User Profile for externalId: {}", userProfileItem.getExternalId()))
            .onErrorResume(WebClientResponseException.NotFound.class, notFound ->
                handleUserNotFound(userId, notFound.getResponseBodyAsString()));
    }

    private Mono<? extends GetUserProfile> handleUserNotFound(String externalId, String responseBodyAsString) {
        log.info("User with externalId: {} does not exist: {}", externalId, responseBodyAsString);
        return Mono.empty();
    }

}
