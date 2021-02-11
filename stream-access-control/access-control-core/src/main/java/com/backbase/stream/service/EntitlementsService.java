package com.backbase.stream.service;

import com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementItem;
import com.backbase.stream.exceptions.UserNotFoundException;
import com.backbase.stream.legalentity.model.AssignedPermission;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.legalentity.model.User;
import com.backbase.stream.product.service.ArrangementService;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

/**
 * Entitlement Service contains functions to query access control .
 */
@Slf4j
@AllArgsConstructor
public class EntitlementsService {

    private final ArrangementService arrangementService;
    private final UserService userService;
    private final AccessGroupService accessGroupService;
    private final LegalEntityService legalEntityService;

    /**
     * Get Assigned permissions for external user for resource and function name with a privilege. First retrieves
     * internal user id to then query master service agreement in order to query assigned permissions
     *
     * @param externalUserId External User ID
     * @param resourceName Business Resource Name (i.e. Transactions)
     * @param functionName The function name to access the resource (i.e.' Transactions)
     * @param privilege Which privilege to query (i.e. view)
     * @return List of assigned permissions
     */
    public Flux<AssignedPermission> getAssignedPermissions(
        String externalUserId,
        String resourceName,
        String functionName,
        String privilege) {

        return userService.getUserByExternalId(externalUserId).flux().flatMap(user ->
            getAssignedPermissionsForUser(resourceName, functionName, privilege, user));
    }

    public Flux<AssignedPermission> getAssignedPermissionsForUser(String resourceName, String functionName,
        String privilege, User user) {
        return legalEntityService.getMasterServiceAgreementForInternalLegalEntityId(user.getLegalEntityId())
            .flux()
            .flatMap(sa ->
                getAssignedPermissionForServiceAgreement(resourceName, functionName, privilege, user, sa));
    }

    private Flux<AssignedPermission> getAssignedPermissionForServiceAgreement(
        String resourceName,
        String functionName,
        String privilege,
        User user,
        ServiceAgreement sa) {
        return accessGroupService.getAssignedPermissions(sa, user, resourceName, functionName, privilege)
            .flatMap(permission ->
                Flux.fromIterable(permission.getPermittedObjectInternalIds())
                    .flatMap(arrangementService::getArrangement)
                    .collectList()
                    .map(products -> setAssignedPermissionForArrangements(permission, products)));
    }

    /**
     * Get All Products a legal entity has access to.
     *
     * @param legalEntityId Internal Legal Entity Id
     * @return List of Products
     */
    public Flux<AccountArrangementItem> getProductsForInternalLegalEntityId(String legalEntityId) {
        return legalEntityService.getMasterServiceAgreementForInternalLegalEntityId(legalEntityId)
            .flux()
            .flatMap(sa -> accessGroupService.getDataGroupItemIdsByServiceAgreementId(sa.getInternalId())
                .flatMap(arrangementService::getArrangement)
                .flatMap(Mono::just)
            );
    }

    /**
     * Get All Products a legal entity has access to.
     *
     * @param legalEntityId External Legal Entity Id
     * @return List of Products
     */
    public Flux<AccountArrangementItem> getProductsForExternalLegalEntityId(String legalEntityId) {
        return legalEntityService.getLegalEntityByExternalId(legalEntityId).flux()
            .flatMap(legalEntity -> getProductsForInternalLegalEntityId(legalEntity.getInternalId()));
    }

    private AssignedPermission setAssignedPermissionForJourneys(AssignedPermission permission, List<String> externalIds,
        ProductGroup.ProductGroupTypeEnum productGroupTypeEnum) {
        permission.setPermittedObjectExternalIds(externalIds);
        return permission;
    }

    private AssignedPermission setAssignedPermissionForArrangements(AssignedPermission permission,
        List<AccountArrangementItem> products) {
        List<String> externalIds = products.stream().map(AccountArrangementItem::getExternalArrangementId)
            .collect(Collectors.toList());
        permission.setPermittedObjects(
            Collections.singletonMap(ProductGroup.ProductGroupTypeEnum.ARRANGEMENTS.name(), products));
        permission.setPermittedObjectExternalIds(externalIds);
        return permission;
    }

    public Mono<Tuple2<User, LegalEntity>> getLegalEntityForUserName(String username) {
        return userService.getUserByExternalId(username)
            .doOnNext(user -> log.info("Found user: {} for username: {}", user.getInternalId(), username))
            .switchIfEmpty(Mono.error(new UserNotFoundException("User not found for username: " + username)))
            .flatMap(user -> {
                return Mono.just(user).zipWith(legalEntityService.getLegalEntityByInternalId(user.getLegalEntityId()));
            });
    }


}
