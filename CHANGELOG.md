# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [2.26.0]
### Changed
- Fix for null point exception when no users are informed during the legal entity bootstrap.
- Enhancements in utility for aggregating all products of a product group.
- Enhanced the documentation for Service Agreements endpoints.

## [2.25.0]
### Added
- Custom Service Agreement to legal entity upsert
- Updated Service Agreement endpoint

## [2.17.0]
### Fixed
- Fix for issue Product Catalog creation fails when either kinds or types are nullCLOSED (Product Catalog null pointer when either kinds or types are null)
- Fix for issue Dependency conflict for org.yaml.snakeyamlCLOSED  (dependency conflicts)
- Fix for null pointer in UserProfileMapper when additional emails/phones are null

## [2.16.0]
### Fixed
- Fix in pipelines (Github Actions) and compile with JDK 11

### Added
- Added UserProfile support (for DBS service user-profile-manager) in Legal Entity SAGA.
It’s disabled by default and can be enabled by setting flag backbase.stream.legalentity.sink.userProfileEnabled to true.
It’s implemented as a nested object under User model, and it will use some of its properties (like fullName, email and phoneNumber) when constructing the UserProfile DBS resource.
It performs an upsert so it supports both creation and update.
Example:

```yaml
- user:
    externalId: sara
    fullName: Sara Jones
    identityLinkStrategy: CREATE_IN_IDENTITY
    emailAddress:
      address: sara@email.com
    mobileNumber:
      number: '1234567890'
    userProfile:
      title: Miss
      personalInformation:
        gender: female
        dateOfBirth: '1995-12-30'
        countryOfBirth: Netherlands
      locale: nl-NL
      additionalEmails:
        - value: sara2@email.com
        - value: sara3@email.com
      additionalPhoneNumbers:
        - value: '012121212'
        - value: '01313713'
      addresses:
        - streetAddress: Fake Street 123
          locality: Utrecht
          country: Netherlands
          type: home
          primary: 'true'
```

## [2.15.0]
### Fixed
- Fixed implementation of Reference Job Roles where we can assign a list of reference job roles to a specific user. 
  * Example with legal-entity-bootstrap-task on how to create a Reference Job Role in the root legal entity and assign it to a user below the hierarchy (example with a subsidiary):
```yaml
bootstrap:
  legalEntity:
    name: "Backbase Bank"
    realmName: "backbase"
    externalId: "backbase-bank"
    legalEntityType: "BANK"
    referenceJobRoles:
      - name: "Entitlements - Manager"
        description: "Full Entitlements administration."
        functionGroups:
          - name: "Manage Entitlements"
            functions:
              - functionId: "1019"
                functionCode: "manage.data.groups"
                privileges:
                  - privilege: "view"
                  - privilege: "create"
                  - privilege: "edit"
                  - privilege: "delete"
                  - privilege: "approve"
              - functionId: "1020"
                functionCode: "manage.function.groups"
                privileges:
                  - privilege: "view"
                  - privilege: "create"
                  - privilege: "edit"
                  - privilege: "delete"
                  - privilege: "approve"
    administrators:
      - externalId: "bbadmin"
        fullname: "Backbase Root Admin"
        identityLinkStrategy: "CREATE_IN_IDENTITY"
        emailAddress:
          address: "bbadmin@email.com"
        mobileNumber:
          number: "1234567890"
    users:
      - user:
          externalId: "bbadmin"
        referenceJobRoleNames:
          - "Entitlements - Manager"
    subsidiaries:
      - name: "Backbase Subsidiary"
        realmName: "backbase"
        externalId: "backbase-sub"
        legalEntityType: "BANK"
        administrators:
          - externalId: "bbsubadmin"
            fullname: "Backbase Subsidiary Admin"
            identityLinkStrategy: "CREATE_IN_IDENTITY"
            emailAddress:
              address: "bbsubadmin@email.com"
            mobileNumber:
              number: "1234567890"
        users:
          - user:
              externalId: "bbsubadmin"
            referenceJobRoleNames:
              - "Entitlements - Manager"
```

## [2.11.0]
### Added
- Packaging the Saga’s OpenAPI specs in a zip module:
```xml
<dependency>
    <groupId>com.backbase.stream</groupId>
    <artifactId>stream-models</artifactId>
    <version>2.11.0</version>
    <classifier>specs</classifier>
    <type>zip</type>
</dependency>
```

## [2.10.0]
### Fixed
- Fixed object mapper to not serialize null values
- BUGFIX: Product Ingestion Saga was replacing all permissions assigned to users and adding new ones, now they are merged: #14

### Changed
- Upgraded Spring Boot to the version in SSDK
- Upgraded BOAT and renamed lots of generated API operationIds that now make more sense (most of them)

### Added
- Added lots and more sensible logging and pretty-printing functions to keep track on what's happening

## [2.9.0]
### Changed
- The master service agreement now can be manipulated during the creation of the Legal Entity, so we can add Assignable Permission Sets to it.

## [2.6.0]
### Added
- DBS 2.19 support
- DBS Lean Services
- Included spring configuration properties support

### Removed
- Removal of MongoDB dependencies

### Changed
- Removing Job Profile Templates
- Upgrade Spring Boot to 2.3.x
- Stream SDK part of Stream Services repository
- DBS Clients generated with Boat
- DBS Endpoints changed, defaults are now set to the defaults setup in Kubernetes.
  * Before:
```yaml
spring:
  security:
    oauth2:
      client:
        provider:
          dbs:
            token-uri: https://stream-demo.proto.backbasecloud.com/api/token-converter/oauth/token
backbase:
  stream:
    dbs:
      access-control-pandp-base-url: https://stream-api.proto.backbasecloud.com/accesscontrol-pandp-service/service-api/v2
      access-group-presentation-base-url: https://stream-api.proto.backbasecloud.com/accessgroup-presentation-service/service-api/v2
      account-presentation-base-url: https://stream-api.proto.backbasecloud.com/account-presentation-service/service-api/v2
      legal-entity-presentation-base-url:  https://stream-api.proto.backbasecloud.com/legalentity-presentation-service/service-api/v2
      user-presentation-base-url: https://stream-api.proto.backbasecloud.com/user-presentation-service/service-api/v2
      transaction-presentation-base-url: https://stream-api.proto.backbasecloud.com/transaction-presentation-service/service-api/v2
```
  * After:
```yaml
spring:
  security:
    oauth2:
      client:
        provider:
          dbs:
            token-uri: http://token-converter:8080/oauth/token
backbase:
  stream:
    dbs:
      user-manager-base-url: http://user-manager:8080
      access-control-base-url: http://access-control:8080
      arrangement-manager-base-url: http://arrangement-manager:8080
      transaction-manager-base-url: http://transaction-manager:8080
      limit-manager-base-url: http://limits-manager:8080
```
[2.26.0]: https://github.com/Backbase/stream-services-2.0/compare/2.25.0...2.26.0
[2.25.0]: https://github.com/Backbase/stream-services-2.0/compare/2.17.0...2.25.0
[2.17.0]: https://github.com/Backbase/stream-services-2.0/compare/2.16.0...2.17.0
[2.16.0]: https://github.com/Backbase/stream-services-2.0/compare/2.15.0...2.16.0
[2.15.0]: https://github.com/Backbase/stream-services-2.0/compare/2.11.0...2.15.0
[2.11.0]: https://github.com/Backbase/stream-services-2.0/compare/2.10.0...2.11.0
[2.10.0]: https://github.com/Backbase/stream-services-2.0/compare/2.9.0...2.10.0
[2.9.0]: https://github.com/Backbase/stream-services-2.0/compare/2.6.0...2.9.0
[2.6.0]: https://github.com/Backbase/stream-services-2.0/releases/tag/2.6.0