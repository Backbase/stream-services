# Changelog
All notable changes to this project will be documented in this file.
## [8.4.1](https://github.com/Backbase/stream-services/compare/8.4.0...8.4.1)
### Changed
- added logger for LegalEntitySaga and AccessGroupService to debug
## [8.4.0](https://github.com/Backbase/stream-services/compare/8.3.0...8.4.0)
### Added
- add the investment service integration; part1
  - clients
  - portfolios
  - portfolio products
  - assets

## [8.3.0](https://github.com/Backbase/stream-services/compare/8.2.10...8.3.0)
### Changed
- Upgrade to SSDK 20.0.0

## [8.2.0](https://github.com/Backbase/stream-services/compare/8.1.1...8.2.0)
### Changed
- Upgraded transaction-manager service-api from v2.11.2 to v3.0.2

## [8.1.1](https://github.com/Backbase/stream-services/compare/8.0.0...8.1.1)
### Changed
- RC Release: Upgrade to SSDK 20.0.0-rc.2

## [8.0.0](https://github.com/Backbase/stream-services/compare/7.8.0...8.0.0)
### Changed
- Remove usages of deprecated access-control service-api/v3
- Migrate to access-control service-api/v1 and integration-api/v1
- Date and time format changed in api/stream-legal-entity/openapi.yaml. Dates are now represented in full ISO-8601 format including time and timezone, e.g. "2023-10-02T14:48:00.000Z"

## [7.8.0](https://github.com/Backbase/stream-services/compare/7.7.0...7.8.0)
### Changed
- Bumping Backbase BOM to 2025.09-LTS
- Upgraded SSDK to 19.1.1

## [7.7.0](https://github.com/Backbase/stream-services/compare/7.6.0...7.7.0)
### Changed
- Add new logic for create approval policy and type to extract internal service agreement id by external service agreement id
- Replace usages of the old create approval policy and type endpoints with the new improved ones
- Unit tests have been updated accordingly
- Add new property for approval type model - serviceAgreementId

## [7.6.0](https://github.com/Backbase/stream-services/compare/7.5.0...7.6.0)
### Changed
- Extended stream-approvals openapi.yaml in case of 'scope' property for the Policy and ApprovalType
- The logic of ApprovalsIntegrationService.createApprovalType() and ApprovalsIntegrationService.createPolicy() was extended to use different endpoints in case scope property presents
- Unit test has been updated accordingly

## [7.5.0](https://github.com/Backbase/stream-services/compare/7.4.0...7.5.0)
### Changed
- Added CustomerAccessGroupSaga
- Updated LegalEntityV2Saga to include CustomerAccessGroup assign
- Updated ServiceAgreementV2Saga to include CustomerAccessGroup assign
- New properties added to enable CAG ingestion with bootstrap (false by default):
```properties
backbase.stream.customer-access-groups.enabled=true|false
backbase.bootstrap.ingestions.cag.enabled=true|false
```
## [7.4.0](https://github.com/Backbase/stream-services/compare/7.3.0...7.4.0)
### Changed
- Add concurrency for job role ingestion 
- New properties for concurrency for job roles ingestion (default values are 1):    
```properties
backbase.stream.access.control.concurrency=1
backbase.stream.legalentity.sink.concurrency=1
```

## [7.3.0](https://github.com/Backbase/stream-services/compare/7.2.0...7.3.0)
### Changed
- Fixed job role ingestion

## [7.2.0](https://github.com/Backbase/stream-services/compare/7.1.0...7.2.0)
### Changed
- Updated Product Mapper
- Updated Test cases for Product Mapper
- 
## [7.0.0](https://github.com/Backbase/stream-services/compare/6.19.0...7.0.0)
### Changed
- Bumping Backbase BOM to 2025.04
- Upgraded SSDK to 19.0.0
- Update boat-maven-plugin to 0.17.62

## [6.19.0] (https://github.com/Backbase/stream-services/compare/6.18.0...6.19.0)
- Integrate Customer Profile Service into Legal Entity Saga and Legal Entity Saga V2 ingestion

## [6.18.0](https://github.com/Backbase/stream-services/compare/6.17.0...6.18.0)
### Added
- Extended the data group types to include; `CASE_DEFINITION`, `CASE_INSTANCE`, `PROCESS_DEFINITION`, `PROCESS_INSTANCE`, `TASK_DEFINITION`, `TASK_INSTANCE`. 

## [6.17.0](https://github.com/Backbase/stream-services/compare/6.16.0...6.17.0)
### Added
- Improved logging for Job Roles ingestion

## [6.16.0](https://github.com/Backbase/stream-services/compare/6.15.0...6.16.0)
### Added
- Debit insurance status support (insuredStatus field)
- Arrangement and Legal Entities relationship now support relations and additions fields

## [6.15.0](https://github.com/Backbase/stream-services/compare/6.14.0...6.15.0)
### Fixed
- APS should be assigned only to TEMPLATE job roles.

## [6.14.0](https://github.com/Backbase/stream-services/compare/6.13.0...6.14.0)
### Fixed
- LegalEntitySaga: Fixed duplicate service agreements from being created for child accounts during login in Family Banking.

## [6.13.0](https://github.com/Backbase/stream-services/compare/6.12.0...6.13.0)
### Added
- add subscriptions for products when it's specified

## [6.12.0](https://github.com/Backbase/stream-services/compare/6.11.0...6.12.0)
### Fixed
- Adds LoB header to user kind segmentation saga in stream audiences

## [6.11.0](https://github.com/Backbase/stream-services/compare/6.10.0...6.11.0)
### Fixed
- Performance improvement on retrieving the user information by calling getUserById (8 ms) Vs getUserByExternalId (50 ms)
- Update ArrangementUpdateException Logger to handle PII data

## [6.10.0](https://github.com/Backbase/stream-services/compare/6.9.0...6.10.0)
### Fixed
- LegalEntitySaga: Fix periodic limits bounds setQuarterly

## [6.9.0](https://github.com/Backbase/stream-services/compare/6.8.0...6.9.0)
### Changed
- fix the logic of setting the job role type for ingestion - see Maint 32629 for more details

## [6.8.0](https://github.com/Backbase/stream-services/compare/6.7.0...6.8.0)
### Changed
- update ssdk version to 18.1.0 - see Maint 32324 for more details

## [6.7.0](https://github.com/Backbase/stream-services/compare/6.6.0...6.7.0)
### Fixed
- Fixed setting internal id for creatorLE in reactive chain

## [6.6.0](https://github.com/Backbase/stream-services/compare/6.5.0...6.6.0)
### Fixed
- Fixed invocation of [putArrangementById](https://backbase.io/developers/apis/specs/arrangement-manager/arrangement-service-api/3.0.5/operations/Arrangements/putArrangementById/) in `ArrangementService` to pass in the arrangement's internalId.
  The arrangement's externalId was erroneously being provided to this method.
- 
## [6.5.0](https://github.com/Backbase/stream-services/compare/6.4.0...6.5.0)
### Fixed
- Fixed productTypeName mapping when ingesting Product data into arrangement-manager

## [6.4.0](https://github.com/Backbase/stream-services/compare/6.3.0...6.4.0)
### Fixed
- Fixed setting internal id for creatorLE before creating SA

## [6.3.0](https://github.com/Backbase/stream-services/compare/6.2.0...6.3.0)
### Fixed
- Fixed missing explicit state mappings for BaseProduct related classes

## [6.2.0](https://github.com/Backbase/stream-services/compare/6.1.0...6.2.0)
### Changed
- update ssdk version to 18.0.1

## [6.1.0](https://github.com/Backbase/stream-services/compare/5.16.0...6.1.0)
### Changed
- Updated to 2024.10 bb bom, and plan-manager to v1

## [5.15.0](https://github.com/Backbase/stream-services/compare/5.15.0...5.15.0)
### Changed
- Updated legal entities specs for service agreement to include purpose field

## [5.14.0](https://github.com/Backbase/stream-services/compare/5.13.0...5.14.0)
### Changed
- Updated arrangement-manager service-api from v2 to v3

## [5.13.0](https://github.com/Backbase/stream-services/compare/5.12.0...5.13.0)
### Changed
- Bumping Backbase BOM to 2024.09-LTS
- Upgraded SSDK to 17.1.0

## [5.12.0](https://github.com/Backbase/stream-services/compare/5.11.0...5.12.0)
### Added
- Adding additional pmts mappings

## [5.11.1](https://github.com/Backbase/stream-services/compare/5.11.0...5.11.1)
### Changed
- Updated plan-manager service api from 0.5.0 to 0.9.0

## [5.11.0](https://github.com/Backbase/stream-services/compare/5.10.0...5.11.0)
### Changed
- feature - improved payment ingestion to allow for joint owner accounts to be shared.

## [5.10.0](https://github.com/Backbase/stream-services/compare/5.9.1...5.10.0)
### Added
- Introducing Grand Central customer canonical model: Processing the `PartyUpsertEvent` using the Legal Entity Saga via the [legal-entity-composition-service](stream-compositions/services/legal-entity-composition-service)

## [5.9.1](https://github.com/Backbase/stream-services/compare/5.9.0...5.9.1)
### Fixed
- Update data groups with custom data items

## [5.9.0](https://github.com/Backbase/stream-services/compare/5.8.0...5.9.0)
### Added
- Added plan-manager for TVP (Tailored Value Proposition)

## [5.8.0](https://github.com/Backbase/stream-services/compare/5.7.0...5.8.0)
### Changed
- make ContactsSaga use continueOnError flag

## [5.7.0](https://github.com/Backbase/stream-services/compare/5.6.0...5.7.0)
### Changed
- bug fixed - missing stream-starter module in stream-sdk

## [5.6.0](https://github.com/Backbase/stream-services/compare/5.5.0...5.6.0)
### Changed
- Removed the service-sdk-starter-reactive since it will be removed soon and created stream-starter with all the dependencies in service-sdk-starter-reactive

## [5.4.0](https://github.com/Backbase/stream-services/compare/5.3.0...5.4.0)
### Changed
- Fix the logic for creating Template type job roles only for MSA

## [5.3.0](https://github.com/Backbase/stream-services/compare/5.2.0...5.3.0)
### Added
- Add CUSTOMERS product group type to OpenAPI contract

## [5.2.0](https://github.com/Backbase/stream-services/compare/5.1.0...5.2.0)
### Added
- Adding login-based ingestion for product composition. Disabled by default, to enable: `backbase.stream.compositions.product.login-event.enabled=true`.
- Upgrade stream composition to 2024.04

## [5.1.0](https://github.com/Backbase/stream-services/compare/5.0.1...5.1.0)
### Changed
- Upgrade stream composition to 2024.03.10-LTS

## [5.0.1](https://github.com/Backbase/stream-services/compare/5.0.0...5.0.1)
### Changed
- Use pagination to query Payment Orders

## [5.0.0](https://github.com/Backbase/stream-services/compare/5.0.0...4.0.0)
### Changed
- Bumping Service SDK to **17.0.0**
- Bumping Banking Services clients to **2024.04**

## [4.1.1](https://github.com/Backbase/stream-services/compare/4.1.1...4.1.0)
### Changed
- update with fixes from 3.72.4
- Upgrade to Backbase version `2024.03-LTS`

## [4.1.0](https://github.com/Backbase/stream-services/compare/4.1.0...4.0.0)
### Changed
- Bumping Service SDK to **16.1.5**

## [4.0.0](https://github.com/Backbase/stream-services/compare/4.0.0...3.70.0)
### Changed
- Bumping Service SDK to **16.0.1**
  - Multiple breaking changes were introduced as part of this upgrade, including Spring Boot 3 upgrade, replacing Spring Sleuth by Micrometer and modules structure for the Composition Events.
- Bumping Banking Services clients to **2023.12**
- Enhancing Multi-tenancy support by removing the concurrency limitation from `v3.70.0` using the new reactor's context propagation.

## [3.72.1](https://github.com/Backbase/stream-services/compare/3.72.0...3.72.1)
### Added
- Change name of transaction cursor mapper bean so that it does not clash with dependencies.

## [3.72.0](https://github.com/Backbase/stream-services/compare/3.71.0...3.72.0)
### Added
- A flag to skip updates to user in Identity `backbase.stream.user.management.update-identity`.

## [3.71.0](https://github.com/Backbase/stream-services/compare/3.70.0...3.71.0)
### Added
- Added new models for LE and SA to cover requirements in the new bootstrap. In the new structure, LE supports just master service agreement and custom service agreement has been dropped. Product groups, job profile users, reference job roles, contacts were moved from LE to new SA model. In the new bootstrap, LE and SA are now separated and can be defined in the different JSON files and independently ingested.  
## [3.70.1](https://github.com/Backbase/stream-services/compare/3.70.0...3.70.1)
### Added
- A flag to skip updates to user in Identity `backbase.stream.user.management.update-identity`.

## [3.70.0](https://github.com/Backbase/stream-services/compare/3.69.0...3.70.0)
### Added
- Support to Events via Azure Service Bus for the Stream Composition Services
- Product Composition now supports transaction-manager's out-of-the-box pulling mechanism, via the `transaction-pull-integration-service`. A separate chain was created for that, enabled via: `backbase.stream.compositions.product.chains.transaction-manager.enabled`
  - Only one of the transaction chains can be enabled, either the `transaction-manager` or `transaction-composition`.
  - The purpose of this new chain is to create better support for the OOTB implementation and enable ModelBank projects.
  - The refresh supports fine graining the pulling requests per arrangement, or a bulk request for all arrangements at once via configuring `splitPerArrangement` and `concurrency` properties.
- Multi-tenancy support to SSDK message broker Events
  - Only supported for `spring.cloud.stream.default.consumer.concurrency=1` - Will be enhanced when upgraded to Service SDK 16
  - **Breaking Change**: Property `backbase.stream.client.headersToForward` is now replaced by `backbase.stream.context.headersToForward`

### Changed
- Bumping Service SDK to **15.2.4**
- Bumping Banking Services clients to **2023.09.17-LTS**
- **Breaking Change**: Stream Composition services Async chains will now use SSDK message broker Events instead of relying in Reactive Subscriptions.
  - This will bring better isolation during events processing and more control in terms of throughput and concurency.
- Enhancing Api Client logs when `logging.level.reactor.netty.http.client=DEBUG`

## [3.69.0](https://github.com/Backbase/stream-services/compare/3.68.0...3.69.0)
### Added
- Add customer category to service agreement spec

## [3.68.1](https://github.com/Backbase/stream-services/compare/3.68.0...3.68.1)
### Changed
- Add AccountArrangementItemPut mapping in ProductMapper

## [3.68.0](https://github.com/Backbase/stream-services/compare/3.67.0...3.68.0)
### Changed
- Add CardsReference to legal-entity openapi spec

## [3.67.0](https://github.com/Backbase/stream-services/compare/3.66.0...3.67.0)
### Changed
- Fix transaction-cursor k8 deployment failure issue: `org.springframework.beans.factory.NoSuchBeanDefinitionException: No qualifying bean of type 'com.backbase.stream.compositions.transaction.cursor.core.mapper.TransactionCursorMapper' available`
- Fix transaction-cursor `application-local.yaml` config

## [3.66.0](https://github.com/Backbase/stream-services/compare/3.65.0...3.66.0)
### Changed
- Always check cursor and create if it does not exist.
- If `DateRangeEnd` is passed in the composition request set that as lastTxnDate instead of system date.

## [3.65.2](https://github.com/Backbase/stream-services/compare/3.65.1...3.65.2)
### Changed
- Adding additional mapping attributes, reservedAmount, to Credit card.

## [3.65.1](https://github.com/Backbase/stream-services/compare/3.65.0...3.65.1)
### Changed
- Query for existing payments by using arrangement IDs instead of user IDs. This will eliminate duplicate payments from being ingested when joint owners are added.

## [3.65.0](https://github.com/Backbase/stream-services/compare/3.64.0...3.65.0)
### Changed
- Move call to processAudiencesSegmentation after setupUsers 

## [3.64.0](https://github.com/Backbase/stream-services/compare/3.63.0...3.64.0)
### Changed
- Return error when transaction composition failed.

## [3.63.0](https://github.com/Backbase/stream-services/compare/3.62.0...3.63.0)
### Changed
- Enabling service discovery for stream composition components, direct endpoint urls were removed.
- To use static uri settings, configure it as the following example:
```yaml
backbase:
  communication:
    services:
      stream:
        legal-entity:
          integration:
            direct-uri: http://legal-entity-integration:8080
        product:
          integration:
            direct-uri: http://product-integration:8080
          composition:
            direct-uri: http://product-composition:8080
        product-catalog:
          integration:
            direct-uri: http://product-catalog-ingestion-integration:8080
        payment-order:
          integration:
            direct-uri: http://payment-order-integration:8080
          composition:
            direct-uri: http://payment-order-composition:8080
        transaction:
          composition:
            direct-uri: http://transaction-composition:8080
          cursor:
            direct-uri: http://transaction-cursor:8080
          integration:
            direct-uri: http://transaction-integration:8080
```
> To keep it retro-compatible you also need to set: `spring.cloud.loadbalancer.enabled=false`

## [3.62.0](https://github.com/Backbase/stream-services/compare/3.61.0...3.62.0)
### Added
- Secondary Service Agreement update
## [3.60.3](https://github.com/Backbase/stream-services/compare/3.60.1...3.60.2)
### Changed
- Enhance Legal Entity level Limit Object to set Limit based on Privilege, Business Function and Legal Entity

## [3.60.2](https://github.com/Backbase/stream-services/compare/3.60.1...3.60.2)
### Changed
- Fix formatting of StreamTask error messages
- Fix message placeholders of `AccessGroupService`

## [3.60.1](https://github.com/Backbase/stream-services/compare/3.60.0...3.60.1)
### Added
- Add populating the user manager cache with user profile data.

## [3.60.0](https://github.com/Backbase/stream-services/compare/3.59.0...3.60.0)
### Added
- Adding the ability to retrieve the arrangement a payment belongs to.
### Changed
- updated application-local configurations to be aligned with the new Backbase Local env.

## [3.59.0](https://github.com/Backbase/stream-services/compare/3.58.2...3.59.0)
### Added
- Add update identity user attributes in case it's previously created.
## [3.58.2](https://github.com/Backbase/stream-services/compare/3.58.1...3.58.2)
### Changed
- Avoiding race condition when assigning a realm to a legal entity when ingesting multiple subsidiaries at the same time.

## [3.58.0](https://github.com/Backbase/stream-services/compare/3.57.0...3.58.0)
### Changed
- For the IMPORT_FROM_IDENTIY strategy use the user manager importIdentity API call

## [3.57.0](https://github.com/Backbase/stream-services/compare/3.56.0...3.57.0)
### Added
- Add feature flag for limits ingestion default is true, `backbase.stream.limits.worker.enabled`
## [3.56.0](https://github.com/Backbase/stream-services/compare/3.55.0...3.56.0)
### Changed
- Fix missing portfolio aggregation model attribute `externalId`
## [3.55.0](https://github.com/Backbase/stream-services/compare/3.54.0...3.55.0)
### Changed
- Removed the no-scs tag used during the docker image build. The latest baas use service bus and these libraries will be required.
## [3.53.0](https://github.com/Backbase/stream-services/compare/3.52.0...3.53.0)
### Changed
- Fixed a bug that was causing the Product Composition integration with Transaction Composition to fail due to a missing required request parameter (`arrangementId`)

## [3.52.0](https://github.com/Backbase/stream-services/compare/3.51.0...3.52.0)
### Changed
- Fixed two small bugs caused by introduction of UserKindSegmentationSage
  add check for LE not having users before processing. (should fix error when LE is empty)
  add AudiencesSegmentationConfiguration to LegalEntitySagaConfiguration

## [3.51.0](https://github.com/Backbase/stream-services/compare/3.50.0...3.51.0)
### Changed
- Upgrading Stream from `2023.06` to `2023.06.2`
- 
## [3.50.0](https://github.com/Backbase/stream-services/compare/3.49.0...3.50.0)
### Changed
- Upgrading Stream from `2023.02-LTS` to `2023.06`
- Updated Portfolio's position stream contract to include new fields

## [3.49.0](https://github.com/Backbase/stream-services/compare/3.48.0...3.49.0)
### Added
- Added Support for ingesting LE customers into audiences user-kind segments (for both Retail and Business banking)
- The customer category is taken from the LE and if not present the default property will be used.
- UserKindSegmentationSage Needs to be enabled explicitly through these properties:
    ```properties
    backbase.stream.audiences.segmentation.user-kind.enabled=true|false
    backbase.stream.audiences.segmentation.user-kind.default-customer-category=RETAIL|BUSINESS
    ``` 

## [3.48.0](https://github.com/Backbase/stream-services/compare/3.47.0...3.48.0)
### Changed
- Updated UserService.createOrImportIdentityUser to populate user's additions to DBS with IMPORT_FROM_IDENTITY linking strategy

## [3.47.0](https://github.com/Backbase/stream-services/compare/3.46.0...3.47.0)
### Changed
- Removed unused parameter from Payment Orders filter request. 
The `from` parameter was set to Integer.MAX_VALUE and that was causing errors on newer Backbase versions. 

## [3.46.0](https://github.com/Backbase/stream-services/compare/3.45.0...3.46.0)
### Added
- Ingestion mode configuration for legal-entity and product compositions

    New properties for `legal-entity-composition`:
    ```properties
    backbase.stream.compositions.legal-entity.ingestion-mode.function-groups=UPSERT|REPLACE
    backbase.stream.compositions.legal-entity.ingestion-mode.data-groups=UPSERT|REPLACE
    backbase.stream.compositions.legal-entity.ingestion-mode.arrangements=UPSERT|REPLACE
    ```

    New properties for `product-composition`:
    ```properties
    backbase.stream.compositions.product.ingestion-mode.function-groups=UPSERT|REPLACE
    backbase.stream.compositions.product.ingestion-mode.data-groups=UPSERT|REPLACE
    backbase.stream.compositions.product.ingestion-mode.arrangements=UPSERT|REPLACE
    ```
    Defaults stay the same as before (`UPSERT` for all)

## [3.45.0](https://github.com/Backbase/stream-services/compare/3.44.0...3.45.0)
### Added
- Added support for loans ingestion into DBS.
- Included the ingestion of loans to BatchProductIngestionSage where productGroups are being processed.
- After related arrangements were created in Products capability the loans part is ingested to Loans.

## [3.44.0](https://github.com/Backbase/stream-services/compare/3.43.0...3.44.0)
### Added
- Added Pull request body(description and checklist) validation workflow

## [3.43.0](https://github.com/Backbase/stream-services/compare/3.42.0...3.43.0)
### Changed
- Upgraded Access-Control service-api from v2 to v3

## [3.42.0](https://github.com/Backbase/stream-services/compare/3.41.0...3.42.0)
### Added
- Added Pull Request template for Stream contributors.

## [3.41.0](https://github.com/Backbase/stream-services/compare/3.40.0...3.41.0)
### Added
- Added additions to `DebitCardItem` openapi schema.

## [3.40.0](https://github.com/Backbase/stream-services/compare/3.39.2...3.40.0)
### Added
- Add support for propagating additions from the legal entity ingestion request.

## [3.39.2](https://github.com/Backbase/stream-services/compare/3.39.0...3.40.1)
### Changed
- Propagate additions returned by the Product and Transaction integration Response to the Product and Transaction composition Response.

## [3.39.1](https://github.com/Backbase/stream-services/compare/3.39.0...3.40.1)
### Changed
- Bugfix for putAssignUserPermissions error `Data groups cannot be duplicated in scope of a single function group during assigning permissions`

## [3.39.0](https://github.com/Backbase/stream-services/compare/3.38.0...3.39.0)
### Added
- Add PermissionSetApi to stream-dbs-clients for APS ingestion scenarios

## [3.38.0](https://github.com/Backbase/stream-services/compare/3.37.0...3.38.0)
### Added
- Updated Product Mapper to map panSuffix to all product types `panSuffix` to `number`

## [3.37.0](https://github.com/Backbase/stream-services/compare/3.36.0...3.37.0)
### Added
- Adds `bankBranchCode` field to `TermDeposit` openapi schema

## [3.36.0](https://github.com/Backbase/stream-services/compare/3.35.0...3.36.0)
### Added
- Adds `bankBranchCode` field to `term-deposits.json` events
 
## [3.35.0](https://github.com/Backbase/stream-services/compare/3.34.0...3.35.0)
### Added
- Support for creating data group of type `CUSTOM`.

## [3.34.0](https://github.com/Backbase/stream-services/compare/3.33.1...3.34.0)
### Changed
- Upgraded SSDK to 15.2.0 
- Upgraded Banking Services to 2023.02-LTS
- Fix some breaking changes introduced by 2023.02-LTS

## [3.30.3](https://github.com/Backbase/stream-services/compare/3.30.2...3.30.3)
### Changed
- Updated stream `InvestmentAccount` schema to include `accountHolderNames`,`accountHolderCountry`,`BIC`

## [3.30.2](https://github.com/Backbase/stream-services/compare/3.30.1...3.30.2)
### Changed
- Updated stream `portfolio.yaml` schema to include `arrangementId`. from 2023.03 it's mandatory for portfolio

## [3.30.1](https://github.com/Backbase/stream-services/compare/3.30.0...3.30.1)
### Changed
- A fix for when zipkin is enabled preventing the boostrap tasks to stop running.

## [3.29.0](https://github.com/Backbase/stream-services/compare/3.28.0...3.29.0)
### Added
- Added 'Update arrangement' functionality (new dedicated endpoint: /service-api/v2/ingest/arrangement/pull and
/service-api/v2/ingest/arrangement/push).

## [3.28.0](https://github.com/Backbase/stream-services/compare/3.27.0...3.28.0)
### Added
- Skipping product ingestion for legal entities without products

## [3.26.4](https://github.com/Backbase/stream-services/compare/3.26.3...3.26.4)
### Changed
- Updated stream `product-catalog/openapi.yaml` schema to include `accountHolderName`

## [3.26.2](https://github.com/Backbase/stream-services/compare/3.26.1...3.26.2)
### Added
- Included property to override the validation of atomic batch responses coming from DBS: When a single item is not successful in the batch response it fails the entire saga. 
Keeping validation enabled for backwards compatibility: `backbase.stream.dbs.batch.validate-atomic-response=true`.
- Enhancing logs for legal entity composition subsidiaries processing.
- Updating the e2e tests images version.

## [3.26.0](https://github.com/Backbase/stream-services/compare/3.25.0...3.26.0)
### Added
- Included support to chain products to all subsidiaries when using legal entity composition.
  Disabled by default for compatibility reasons: `backbase.stream.compositions.legal-entity.chains.include-subsidiaries=false`
- Adjusting default services ports to 8080 to avoid misconfiguration when compositions services are invoking sagas: `backbase.communication.http.default-service-port=8080`

## [3.24.3](https://github.com/Backbase/stream-services/compare/3.24.2...3.24.3)
### Fixed
- Fix portfolio not to stop processing on error

## [3.24.2](https://github.com/Backbase/stream-services/compare/3.24.1...3.24.2)
### Changed
- Updated stream `instrument.yaml` schema to include `iconUrl`

## [3.23.2](https://github.com/Backbase/stream-services/compare/3.23.1...3.23.2)
### Added
- Fixed `currentInvestment` mapping to `currentInvestmentValue` for InvestmentAccount.

## [3.23.1](https://github.com/Backbase/stream-services/compare/3.23.0...3.23.1)
### Added
- Add the population of optional user additions to the service used by user ingestion in identity integration mode.
  User additions are result of data extension to the OOTB user manager service.

## [3.23](https://github.com/Backbase/stream-services/compare/3.22.0...3.23)
### Added
- Fixed current investment field mapping for the InvestmentAccount

## [3.22.0](https://github.com/Backbase/stream-services/compare/3.21.0...3.22.0)
### Added
- Adds new parameter `source` to `ProductIngestPullRequest`, `ProductIngestPushRequest` & `ProductIngestResponse`.
  - `source` field can be useful to understand from where this ingestion process was triggered & can be useful in many ways to identify the source of the request.
- Adds different products to `ProductCompletedEvent`. I.e Loan, Term Deposit, Credit Card & Current Account on top of Savings Account.
- Adds `source` field to `ProductCompletedEvent` & populate it with value sent from the request.
- Adds key fields relate to User ID, Legal Entity ID & Service Agreement ID to `ProductCompletedEvent` & populate it from the `ProductIngestPullRequest` for the ingestion mode Pull.
  - These fields are `userExternalId`, `userInternalId`, `legalEntityExternalId`, `legalEntityInternalId`, `serviceAgreementExternalId`, `serviceAgreementInternalId`.
  - These can be useful on event handler side to identify user & service agreement for which this ingestion was triggered.
- Pass `additions` from `ProductIngestPullRequest` to `ProductIngestResponse` & then to `ProductCompletedEvent` which can be useful to pass any extra information from request & can be captured on event handler.

## [3.20.0](https://github.com/Backbase/stream-services/compare/3.19.0...3.20.0)
### Added
- Additional groups added as attribute to User model and will be passed to legal-entity-integration service within Legal Entity Saga execution

## [3.18.0](https://github.com/Backbase/stream-services/compare/3.17.0...3.18.0)
### Added
- Added MySQL Dependency to fix cursor execution in local env
- Added local profile for Approval Task
- Fixed cursor port number
- Set payment order composition config to false

## [3.17.0](https://github.com/Backbase/stream-services/compare/3.16.0...3.17.0)
### Added
- Added ingestion mode config for product-composition-service

## [3.16.0](https://github.com/Backbase/stream-services/compare/3.15.0...3.16.0)
### Changed
- Upgraded to SSDK 15.1.0 (pinned spring-cloud-kubernetes version to 2.1.13)
- Upgraded to Banking Services 2022.10.3

## [3.15.1](https://github.com/Backbase/stream-services/compare/3.15.0...3.15.1)
### Changed
- Adding null checks for ingesting legal entity, service agreement and arrangements with no users - User pre enrolment scenario.

## [3.15.0](https://github.com/Backbase/stream-services/compare/3.14.1...3.15.0)
### Added
- Add **portfolio-http**
- Add `POST /portfolios/regions/batch` endpoint to portfolio
- Add `POST /portfolios/asset-classes/batch` endpoint to portfolio
- Add `POST /portfolios/batch` endpoint to portfolio
- Add `POST /portfolios/sub-portfolios/batch` endpoint to portfolio
- Add `POST /portfolios/allocations/batch` endpoint to portfolio
- Add `POST /portfolios/valuations/batch` endpoint to portfolio
- Add `POST /portfolios/transaction-categories/batch` endpoint to portfolio
- Add `POST /portfolios/instruments/batch` endpoint to portfolio
- Add `POST /portfolios/hierarchies/batch` endpoint to portfolio
- Add `POST /portfolios/positions/batch` endpoint to portfolio
- Add `POST /portfolios/transactions/batch` endpoint to portfolio

## [3.14.1](https://github.com/Backbase/stream-services/compare/3.14.0...3.14.1)
### Changed
- Cherry-picking fixes from 3.7.1 for stream composition payment order mapping.

### Added
- Adding ReDoc documentation for Stream Compositions APIs

## [3.11.0](https://github.com/Backbase/stream-services/compare/3.10.1...3.11.0)
### Changed
- Replaced BatchProductGroupTask.IngestionMode with more flexible BatchProductIngestionMode class. 
New class keeps ingestion modes separately for each main resource involved in BatchProductIngestionSaga processing: 
function groups, data groups and arrangements. Two preset modes have been created: BatchProductIngestionMode.UPSERT and 
BatchProductIngestionMode.REPLACE (equivalents of previous UPDATE and REPLACE, respectively), but new ones can be 
composed of any "sub modes" combination.

## [3.10.1](https://github.com/Backbase/stream-services/compare/3.10.0...3.10.1)
### Changed
- Adjusting property `backbase.stream.client.headers-to-forward` to take precedence over `backbase.stream.client.additional-headers`.
- Fixing Identity m10y configuration on e2e-tests with properly separated realms.

## [3.10.0](https://github.com/Backbase/stream-services/compare/3.9.3...3.10.0)
### Added
- Zip layout to composition services. This allows the addition of external jars to the classpath using `loader.path` property.

## [3.9.3](https://github.com/Backbase/stream-services/compare/3.9.2...3.9.3)
- Fix memory leak with UnitOfWorkExecutor

## [3.9.2](https://github.com/Backbase/stream-services/compare/3.9.1...3.9.2)
### Added
- End-to-end tests for the Bootstrap Task

## [3.9.0](https://github.com/Backbase/stream-services/compare/3.8.2...3.9.0)
### Changed
Adding SSDK service discovery mechanism to the Stream Task and Http applications.
All the service url properties prefixed by `backbase.stream.dbs.*` and `backbase.stream.identity.*` are now removed
and replaced by the service discovery mechanism of your choice, where the Banking Services will be discovered automatically.

Using Eureka/Registry (Enabled by default):
```properties
eureka.client.serviceUrl.defaultZone=http://registry:8080/eureka
eureka.instance.non-secure-port=8080
```
Using Kubernetes: 
```properties
eureka.client.enabled=false
spring.cloud.kubernetes.enabled=true
```

If you **don't want to user a service discovery** mechanism, the following configuration below needs to be **replaced**. e.g.
```yaml
backbase:
  stream:
    dbs:
      access-control-base-url: http://non-discoverable-host:8080/access-control
    identity:
      identity-integration-base-url: http://non-discoverable-host:8080/identity-integration-service
```
Similar behaviour can be achieved with:
```yaml
eureka:
  client:
    enabled: false
spring:
  cloud:
    discovery:
      client:
        simple:
          instances:
            access-control:
              - uri: http://non-discoverable-host:8080
                metadata:
                  contextPath: /access-control
            identity-integration-service:
              - uri: http://non-discoverable-host:8080
                metadata:
                  contextPath: /identity-integration-service
```

> **Heads Up!**: The Stream Composition services still don't support client load balancing, hence service discovery isn't available for the moment then you can't configure the spring cloud discovery simple instances. In the scenario where your service don't support, or you want to disable client side load balancers (e.g. `spring.cloud.loadbalancer.enabled=false`), you can override the default DBS services addresses using the `direct-uri` property. e.g.
> ```properties 
> backbase.communication.services.access-control.direct-uri=http://non-discoverable-host:8080/access-control
> backbase.communication.services.identity.integration.direct-uri=http://non-discoverable-host:8080/identity-integration-service
> ```
> All configuration properties prefixes can be found at [stream-dbs-clients](stream-dbs-clients/src/main/java/com/backbase/stream/clients/config) module, and they are compliant to SSDK [configuration properties](https://community.backbase.com/documentation/ServiceSDK/latest/generate_clients_from_openapi).

## [3.8.2](https://github.com/Backbase/stream-services/compare/3.8.1...3.8.2)
### Fixed
- Added InterestDetails to BaseProduct

## [3.8.0](https://github.com/Backbase/stream-services/compare/3.7.0...3.8.0)
### Changed
- Upgraded to SSDK 15.0.1
- Upgraded to Java 17
- Creating the [`stream-bootstrap-task`](helm/README.md) for deployment of boostrap task Jobs on Kubernetes.

## [3.6.0](https://github.com/Backbase/stream-services/compare/3.5.0...3.6.0)
### Added
- Add support for Entitlement Wizard Metadata.

## [3.5.0](https://github.com/Backbase/stream-services/compare/3.4.0...3.5.0)
### Added
- Added support for push ingestion mode for product and transactions 

## [3.4.0](https://github.com/Backbase/stream-services/compare/3.3.0...3.4.0)
### Added
- Enable Multi architecture docker images: arm64 and amd64

## [3.3.0](https://github.com/Backbase/stream-services/compare/3.1.0...3.3.0)
### Changed
- Tech Debt: Make portfolio saga idempotent #172

## [3.1.0](https://github.com/Backbase/stream-services/compare/3.0.0...3.1.0)
### Changed
- Upgraded to DBS 2022.09

## [3.0.0](https://github.com/Backbase/stream-services/compare/2.88.0...3.0.0)
### Added
- We welcome [Stream Compositions](stream-compositions)! More details can be found in the [confluence page](https://backbase.atlassian.net/wiki/spaces/ES/pages/3481894959/Stream+Services+3.0).

## [2.86.1](https://github.com/Backbase/stream-services/compare/2.86.0...2.86.1)
### Fixed
- Added qualifier for WebClient in ContactsServiceConfiguration.

## [2.86.0](https://github.com/Backbase/stream-services/compare/2.85.0...2.86.0)
### Fixed
- Custom Job Role Mapping Issue is fixed by adding missing mapping of BusinessFunction object of BusinessFunctionGroupMapper class

## [2.85.0](https://github.com/Backbase/stream-services/compare/2.84.0...2.85.0)
### Added
- Deploying task executables and http services as docker images using `repo.backbase.com/backbase-stream-images` registry. 
> e.g. `repo.backbase.com/backbase-stream-images/legal-entity-bootstrap-task:2.85.0`

### Fixed
- Logging configuration was broken in some modules given wrong dependency scope for `stream-test-support`.

### Changed
- Segregating `moustache-bank` profile in `moustache-bank-subsidiaries` to support ingesting the root legal entity without any dependency on product service.

## [2.84.0](https://github.com/Backbase/stream-services/compare/2.83.0...2.84.0)
### Added
- Contacts Support Added for Legal Entity, Service Agreement and Users 
- Usage Sample of Bootstrap json to be added to Legal Entity, Service Agreement and User 

- Contacts Support for LE Contacts
```yaml
name: ABC Company
legalEntityType: CUSTOMER
contacts:
    - category: Employee
      externalId: a8141b9e06621c312001
      addressLine1: 410 7th St
      addressLine2: ''
      streetName: ''
      postCode: 93950
      town: Pacific Grove
      countrySubDivision: CA
      country: US
      name: Beatrice D. Ma
      contactPerson: N/A
      phoneNumber: 530 676 8602
      Email: be@mail.com
      accounts:
      - externalId: a8141b9e06632d362001
        name: Checking USD 2247
        alias: My account
        accountNumber: '9948772699182247'
        bankName: CitiBank
        bankAddressLine1: 736 Levy Court
        bankAddressLine2: ''
        bankStreetName: ''
        bankPostCode: '01720'
        bankTown: Acton
        bankCountrySubDivision: MA
        bankCountry: US
        BIC: CITIUS33
        bankCode: '11103093'
```

- Contacts Support for User Contacts
```yaml
name: John
realmName: customer
externalId: john
legalEntityType: CUSTOMER
users:
- user:
    externalId: john99
    fullName: John Doe
  referenceJobRoleNames:
  - Retail Customer
  contacts:
  - category: Employee
    externalId: a8141b9e06621c12001
    addressLine1: 736 Levy Court
    addressLine2: ''
    streetName: ''
    postCode: '01720'
    town: Acton
    countrySubDivision: MA
    country: US
    name: Barbara P. Dolan
    contactPerson: N/A
    phoneNumber: 617 509 6995
    Email: Barbara@barb.com
    accounts:
    - externalId: a8141b9e06632d62001
      name: Checking USD 0023
      alias: ''
      accountNumber: '9249194950590023'
      bankName: CitiBank
      bankAddressLine1: 736 Levy Court
      bankAddressLine2: ''
      bankStreetName: ''
      bankPostCode: '01720'
      bankTown: Acton
      bankCountrySubDivision: MA
      bankCountry: US
      BIC: CITIUS33
      bankCode: '11103093'
    - externalId: a8141b9e06632d62002
      name: Checking USD 4858
      alias: ''
      accountNumber: '1445192940594858'
      bankName: CitiBank
      bankAddressLine1: 736 Levy Court
      bankAddressLine2: ''
      bankStreetName: ''
      bankPostCode: '01720'
      bankTown: Acton
      bankCountrySubDivision: MA
      bankCountry: US
      BIC: CITIUS33
      bankCode: '11103093'
```

- Contacts Support for Service Agreement
```yaml
name: Bory Coffee Company Ltd
customServiceAgreement:
  externalId: salary_bory_csa
  name: Salary Services for Bory Coffee Company
  description: Custom Service Agreement Between Salary Services and Bory Coffee Company
  status: ENABLED
  isMaster: 'false'
  participants:
  - externalId: bory-coffee-ltd
    sharingUsers: false
    sharingAccounts: true
    admins:
    - kristelcfo
  - externalId: salary-services-ltd
    sharingUsers: true
    sharingAccounts: false
    users:
    - hhsa01
    - fbsa02
  contacts:
  - category: Employee
    externalId: a8141b9e06621c512001
    addressLine1: 02 Meadows Dr,
    addressLine2: ''
    streetName: ''
    postCode: 30010
    town: Columbus
    countrySubDivision: GA
    country: US
    name: Troy M. Hazard
    contactPerson: N/A
    phoneNumber: 530 676 5523
    Email: Tr@mail.com
    accounts:
    - externalId: a8141b9e06632d562001
      name: Checking USD 0022
      alias: ''
      accountNumber: '2512948500122022'
      bankName: Bank of America
      bankAddressLine1: 50 Georgia St
      bankAddressLine2: ''
      bankStreetName: ''
      bankPostCode: '30102'
      bankTown: Atlanta
      bankCountrySubDivision: GA
      bankCountry: US
      BIC: BOFAUS6H
      bankCode: '121000358'
```
## [2.83.0]
### Fixed
- Adding Custom Role Permission Issue

## [2.82.0]
### Fixed
- Adding fallback to default settings for services endpoints.

## [2.81.0]
### Fixed
- [176](https://github.com/Backbase/stream-services/issues/176): Update Job Role does not consider the 207 multi-status response

## [2.80.0]
### Added
- Support for creating data group of type `REPOSITORIES`.
```yaml
referenceJobRoles:
  - name: Custom Engagement Template Viewer
    description: View Custom Engagement Default Templates
    functionGroups:
      - name: Custom Engagement Template Viewer
        functions:
          - functionId: '1100'
            name: Manage Content
            privileges:
              - privilege: view
productGroups:
  - name: Repository_Group_Template_Custom
    description: Repository group that provides view access to the repository where custom engagement default templates are stored
    productGroupType: REPOSITORIES
    customDataGroupItems:
      - internalId: template-custom
    users:
      - user:
          externalId: emp-john
          fullName: John Doe
        referenceJobRoleNames:
          - Custom Engagement Template Viewer
```

## [2.78.0]
### Added
- Support for updating Portfolio Capability data. Example([stream-portfolio/readme.md](stream-portfolio/readme.md#Bootstrap Ingestion Configuration))

## [2.76.0]
### Added
- Support for LE limits
```yaml
name: Bory Breweries Ltd
legalEntityType: CUSTOMER
limit:
  currencyCode: USD
  transactional: 10000
  daily: 250000
  weekly: 500000
  monthly: 2000000
  quarterly: 600000
  yearly: 1200000
```
- Support for SA limits
```yaml
name: Bory Breweries Ltd
legalEntityType: CUSTOMER
masterServiceAgreement:
  limit:
    currencyCode: USD
    transactional: 10000
    daily: 250000
    weekly: 500000
    monthly: 2000000
    quarterly: 600000
    yearly: 1200000
```
- Support for LE in SA limits
```yaml
name: Bory Breweries Ltd
legalEntityType: CUSTOMER
masterServiceAgreement:
  participants:
    - externalId: bory-brew-ltd
      limit:
        currencyCode: USD
        transactional: 10000
        daily: 250000
        weekly: 500000
        monthly: 2000000
        quarterly: 600000
        yearly: 1200000
```
```yaml
name: Bory Breweries Ltd
legalEntityType: CUSTOMER
customServiceAgreement:
  participants:
    - externalId: bory-brew-ltd
      limit:
        currencyCode: USD
        transactional: 10000
        daily: 250000
        weekly: 500000
        monthly: 2000000
        quarterly: 600000
        yearly: 1200000
```
- Support for Job role limits
```yaml
jobRoles:
  - name: Custom Accounts and Payments
    description: Custom Accounts and Payments
    functionGroups:
      - name: Products, payments, txn, contacts, actions, user profile, devices
        functions:
          - functionId: '1017'
            name: US Domestic Wire
            privileges:
              - privilege: create
                supportsLimit: true
                limit:
                  currencyCode: USD
                  daily: 100000
                  weekly: 400000
                  transactional: 10000
```
```yaml
referenceJobRoles:
  - name: admin
    description: Admin
    functionGroups:
      - name: admin
        functions:
          - functionId: '1017'
            name: US Domestic Wire
            privileges:
              - privilege: create
                supportsLimit: true
                limit:
                  currencyCode: USD
                  daily: 100000
                  weekly: 400000
                  transactional: 10000
```
- Support for User Job role limits
```yaml
referenceJobRoles:
  - name: Domestic Payments
    description: Domestic Payments
    functionGroups:
      - name: Products, payments, txn, contacts, actions, user profile, devices
        functions:
          - functionId: '1017'
            name: US Domestic Wire
            privileges:
              - privilege: approve
                limit:
                  currencyCode: USD
                  transactional: 15000
productGroups:
  - internalId: bblicdag1
    name: My business salary account
    description: The account of my business I use for salary payments
    users:
      - user:
          externalId: hhsa01
          fullName: Henk Hurry
          supportsLimit: true
        referenceJobRoleNames:
          - Domestic Payments
```

## [2.75.0]
Clean up of many old components and replaced Stream SDK with Service SDK 14
> By moving to Service SDK, pipelines can now be configured like any other Backbase service using the Service SDK
>
> **Migrate your CICD pipelines to the Service SDK standards**

### Removed
- Old Legal Entity Open API definitions
- Stream Transactions Open API Spec
- Removed Spring Cloud Data Flow components as nobody uses it
  - Stream Cursor Source
  - Legal Entity Sink
  - Product Sink
  - Transactions Sink
  - Transactions HTTP
- Removed Stream SDK Starters
  - `stream-aio-starter-parent` (replaced by `service-sdk-core-starter`)
  - `stream-batch-starter-parent` (replaced by `service-sdk-starter-core` + `spring-boot-starter-batch`)
  - `stream-generated-client-starter-parent`
  - `stream-processor-starter-parent`
  - `stream-sdk-starter-core`(replaced by `service-sdk-starter-core`)
  - `stream-sink-starter-parent`
  - `stream-source-starter-parent`
- Removed `stream-dbs-web-client` (replaced by `service-sdk-web-client`)
  - The OAuth2 client (provider and registration) initially defined as `dbs` is now called `bb`, hence the token converter configuration should to be updated (e.g. `spring.security.oauth2.client.provider.bb.token-uri=http://token-converter:8080/oauth/token`).

### Changed
- Replaced Stream SDK with Service SDK 14.1.0.
  - Upgrade Spring Boot 2.6.6

## [2.74.0]
### Fixed
- Fix allowing empty product-groups to be created.

## [2.73.0]
### Fixed
- Fix to add an arrangement in more than one product group

## [2.72.0]
### Changed
- Update Spring Boot to 2.5.14
- Update Swagger Core to 2.2.0
- Update bcprov-jdk15on to 1.70 

## [2.71.0]
### Added
- Support for updating Legal Entity data (name and additional fields)
- Support for updating User data

## [2.70.1]
### Fixed
- Fix for issue https://github.com/Backbase/stream-services/issues/138

## [2.71.0]
### Changed
- Upgraded to DBS 2022.04
- Upgrade Spring Boot to 2.15.13

## [2.69.0]
### Added
- Ability to configure what Function Group type needs to be deleted. This can be configured through `backbase.stream.deletion.functionGroupItemType`. With values `NONE` (default) or `TEMPLATE`.

### Fixed
- When deleting a legal entity, it now will iterate over all found users to be deleted.

## [2.68.0]
### Added
- Added debitCards to Savings Account in LegalEntity spec.

## [2.67.0]
### Added
- Added cardDetails to Credit Card in LegalEntity spec.


## [2.66.0]
### Changed
- Order of product group stream task processing within legal entity saga is changed to sequential. This is due to the fact that in some 
  circumstances user permissions update loosing previously assigned permissions during ingestion process (due to the nature of reactive processing) 
### Fixed
- Additional headers propagation to several calls within legal entity saga

## [2.65.2]
### Changed
- Update Spring Boot to 2.5.12
> [CVE-2022-22965: Spring Framework (Spring4Shell)](https://developer.backbase.com/security/CVE-2022-22965-spring-framework-spring4shell)
- Update Spring Cloud Function to 3.1.7
> [CVE-2022-22963: Spring Cloud Function RCE](https://developer.backbase.com/security/cve-2022-22963_spring_cloud_function_rce)

## [2.65.1]
### Changed
- Administrative changes: Updating documentation and cleaning specs.

## [2.65.0]
### Added
- Additional realm roles added as attribute to User model and will be passed to legal-entity-integration service within Legal Entity Saga execution

## [2.64.0]
### Fixed
- JUnit and Hibernate Validator dependency update to address security vulnerabilities.

## [2.62.0]
### Fixed
- Legal Entity Saga: linkLegalEntityToRealm method executed multiple times ( when multiple users are ingested): `unique constraint (PK_LE_ASSIGN_REALM) violated`

## [2.61.0]
### Fixed
- Legal Entity Saga: referenceJobRoleNames are mixed up between users ( when multiple users are ingested)

## [2.52.0]
### Removed 
- Audit Core & Http Service. Created as a demo, never to be used.
- Erroneous log message removed when assigning permissiosn without datagroups which is not an exception anymore. 
### Changed
- UserService
  - Failed operations in User Service now generally return StreamTaskExceptions allowing for better control and handling of failures.
- Stream Task
  - Added last error message for easier logging down stream
- LIngesting a Flux of Customers / Transaction will not cause a thread mayhem and can be controlled through configuration. For Legal Entity Http Serivce, set the `backbase.stream.legalentity.sink.task-executors` property to control. When processing a Flux of Legal Entities, provide the concurrency parameter when invoking the `com.backbase.stream.LegalEntitySaga.executeTask` method. For example :
```
        Flux<LegalEntity> flux = legalEntity
            .map(LegalEntityTask::new)
            .flatMap(legalEntitySaga::executeTask, legalEntitySagaConfiguration.getTaskExecutors())
            .map(LegalEntityTask::getData)
            .doOnNext(actual -> log.info("Finished Ingestion of Legal Entity: {}", actual.getExternalId()));
 ```
## [2.51.0]
### Maintenance
- Update to 2021.10
- Update Spring Boot to 2.5.5
- Update Spring Cloud to 2020.0.4
- Aligned versions across project

## [2.48.0]
### Changed
- Update to `2021.09` release (DBS `2.21.2` and Identity `1.9.2`)
  - Optional parameter (`xTransactionsServiceAgreementId`) added to TransactionPresentationServiceApi.postTransactions.
  - Optional parameter (`xTransactionsServiceAgreementId`) added to TransactionPresentationServiceApi.getTransactions.
  - Optional parameter (`xTransactionsServiceAgreementId`) added to TransactionPresentationServiceApi.patchTransactions.
  - Optional parameter (`xTransactionsServiceAgreementId`) added to TransactionPresentationServiceApi.postDelete.
  - Optional parameter (`xTransactionsServiceAgreementId`) added to TransactionPresentationServiceApi.postRefresh.
### Maintenance
- Updated boat-maven-plugin to `0.15.0`

## [2.47.0]
### Added
- Added support for json logging via logstash-logback-encoder. (could be replaced by service-sdk-starter-logging later on)
In order to have logging in json format it's possible to provide logback.xml config from the external app 
via jvm option `-Dlogging.config=file:logback.xml` or specify in the `application.yml` like `logging.config=file:logback.xml`

## [2.46.3]
### Changed
- Added support for Spring Configuration Server in Tasks and Batch starters
- Added support for Distributed Tracing (sleuth, micrometer) in Task and Batch starters
- Improved exception management on Identity operations
- Fixed Spring Cloud Function definition in Transactions Sink


## [2.46.2]
### Changed
- Non existing Business function groups from the request should be persisted.  

## [2.46.1]
### Changed
- Setting consistent DBS default values for access control token used by delete operations.

## [2.46.0]
### Fixed
- Fix for issue https://github.com/Backbase/stream-services/issues/74 : Reference Job Roles updated in LegalEntitySaga
with empty functions.
- Exclude old snakeyaml dependency
- UserService: improved Exception management
### Added
- SCDF plugin to Transactions Sink
- Functional programming for Transaction Sink

## [2.45.0]
### Fixed
- Legal Entity Saga 
  - Errors happening in the user profile manager must now correctly be dealt with.
  - Ensure reactive immutability on user service operations. 
### Changed
- Use backbase bom pom instead of banking-service and identity boms
- Update to 2021.07 release (DBS 2.21.0 and Identity 1.9.0)
  - Optional parameter (`includeSubscriptions`) added to ArrangementsApi.getArrangementById.
  - Optional parameter (`xTransactionsUserId`) added to TransactionPresentationServiceApi.postTransactions.
  - Optional parameter (`xTransactionsUserId`) added to TransactionPresentationServiceApi.getTransactions.
  - Optional parameter (`xTransactionsUserId`) added to TransactionPresentationServiceApi.patchTransactions.
  - Optional parameter (`xTransactionsUserId`) added to TransactionPresentationServiceApi.postDelete.
  - Optional parameter (`xTransactionsUserId`) added to TransactionPresentationServiceApi.postRefresh.
### Maintenance 
- Upgrade Spring Boot 2.5.3

## [2.44.0]
### Maintenance 
- Cleaned up versions for boat-maven-plugin
- Added spring-boot-configuration-processors to modules that have configuration classes
- Moved Product Catalog Model to own package to prevent deep transitive dependencies
- Removed scdf-maven-plugin. This is now a configuration per project if required

## [2.43.0]
### Fixed
- bugfix NPE for AccessGroupService.getUpdatePermissions  

## [2.42.0]
### Fixed
- Permissions assignment for user marked as admin was working incorrectly due to fact that System Function Group was returned when no assigned groups expected

## [2.41.0]
### Changed
- Add filter to forward `X-TID` headers

## [2.40.0]
### Fixed
- Fix upserting existing Business function
> Note: The field `name` is mandatory for the object `function` is required for the update - alongside the `functionId`.

e.g.
```yaml
jobRoles:
- name: SUUS
  description: Manager of the online helpdesk and processing teams, able to set up/edit contracts, add accounts etc. Also able to set up Broadcast messages
  functionGroups:
    - name: Manage Product Summary
      functions:
        - functionId: 1006
          name: Product Summary
          privileges:
            - privilege: view
            - privilege: create
            - privilege: edit
```

## [2.39.0]
### Changed
- For *access-control-core* exclusion of system function group was added in order to avoid DB constraint triggering during permissions PUT

## [2.38.0]
### Fixed
- For *legal-entity-bootstrap-task* fix case when link le to realm task could end the flow because empty returned instead of chaining stream task


## [2.37.0]
### Added
- For *legal-entity-bootstrap-task*, in case of no users/administrators is specified and identity integration enabled, provided realm will be created (if not exists) and linked to legal entity


## [2.35.0]
### Removed
- Spring Config Server. Superseded by standardized Spring Kubernetes Configuration
### Upgraded
- Spring Boot from 2.3.3.RELEASE to 2.5.0
- Spring Cloud 2020.0.2


## [2.34.0]
### Added
- Included a new approvals saga implemented by the Approvals Bootstrap Task.
 > Please be aware that this saga is not idempotent due to some product limitations.
 > Mor information in the wiki page
 > an example on how to configure it is found below

```yaml
bootstrap:
  approvals:
    - name: 4 eye approval policy
        approvalTypes:
          - name: Supervisor
            description: Supervisor approval level
            rank: 1
          - name: HelpDesk
            description: Digital helpdesk and Operations User
            rank: 2
        policies:
          - name: 4 eye policy
            description: Policy that requires approval from supervisor
            logicalItems:
              - rank: 1
                items:
                  - approvalTypeName: Supervisor
                    numberOfApprovals: 1
              - rank: 2
                operator: OR
                items:
                  - approvalTypeName: HelpDesk
                    numberOfApprovals: 2
        policyAssignments:
          - externalServiceAgreementId: sa_backbase-bank
            policyAssignmentItems:
              - functions:
                  - Assign Permissions
                bounds:
                  - policyName: 4 eye policy
              - functions:
                  - Manage Data Groups
                bounds:
                  - policyName: 4 eye policy
            approvalTypeAssignments:
              - approvalTypeName: Supervisor
                jobProfileName: SUUS
              - approvalTypeName: HelpDesk
                jobProfileName: Digital helpdesk and Operations User
```

## [2.33.0]
### Added
- Add 'build-helper-maven-plugin' for adding the generated sources (i.e. Openapi) as project's source directories.

## [2.32.0]
### Added
- added lock identity user on creation flag.
> New conditionally mandatory property added in legal entity stream: `backbase.stream.identity.identity-integration-base-url` to indicate BackBase Identity base URL for clients that integrate with it. This property must be defined when: `backbase.stream.legalentity.sink.use-identity-integration=true`

## [2.31.0]
### Changed
- Fixing the function group delete to only happen when it is not template type (this is when using referenceJobRoles )

## [2.30.0]
### Changed
- Fixing NullPointerException while creating data group using products i.e. without custom data group id

## [2.29.0]
### Changed
- Checking the response recieved from Legal entity api , user  api to is2xxSuccessful as it  returns 207 response for success.

## [2.28.0]
### Changed
- Add "Custom Data Group Items" to Product Groups, require either Products or Custom Data Group Items (previously just Products)

## [2.27.0]
### Fixed
- Fix for issue https://github.com/Backbase/stream-services/issues/46
```
While deleting a legal entity , we are trying to get user information by sending an internal id to
service-api/v2/users/externalids/{externlaIId}?skipHierarchyCheck=true instead of /service-api/v2/users/{internalId}.
The service-api/v2/users/externalids/{externlaIId}?skipHierarchyCheck=true returns 404 user not found as it is an internal ID and the deleting of the user fails.
```

## [2.26.0]
### Changed
- Fix for null point exception when no users are informed during the legal entity bootstrap.
- Enhancements in utility for aggregating all products of a product group.
- Enhanced the documentation for Service Agreements endpoints.

## [2.25.0]
### Added
- Included possibility to use Custom Service Agreements. From now on, if a custom service agreement is declared, and a master service agreement is not explicitly declared, that legal entity will have a custom service agreement only. If none are declared a default master service agreement is created. e.g.:
```yaml
bootstrap:
  legalEntity:
    name: "Backbase Bank"
    realmName: "backbase"
    externalId: "backbase-bank"
    legalEntityType: "BANK"
    customServiceAgreement:
      name: "backbase-bank"
      description: "backbase bank custom service agreement"
      externalId: "sa_backbase-bank"
      jobRoles:
        - name: SUUS
          description: Manager of the online helpdesk and processing teams, able to set up/edit contracts, add accounts etc. Also able to set up Broadcast messages
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
```
- Created Service Agreement endpoint do updated an existing service agreement. e.g. add more participants to it.
> - PUT <legal-entity-http>/service-agreement
> - PUT <legal-entity-http>/async/service-agreement
> - GET <legal-entity-http>/async/service-agreement/{unitOfWorkId}

## [2.24.0]
### Fixed
- Add missing mapping for `accountHolderNames` in Product Ingestion SAGA

## [2.23.0]
### Changed
- Replace DBS RAML specs (converted to OpenAPI with BOAT) to OOB OpenAPI specs for DBS 2.20.0
- Fix all the mismatches and changes for generated code from new specs
- Upgrade jib base image to distroless java 11
- Upgrade to BOAT 0.14.0

## [2.22.0]
- Wrong commit that was reverted later

## [2.21.0]
### Fixed
- Add missing 'User' schema to the 'additions' transformer in DBS clients

## [2.20.0]
### Fixed
- Make `upsertArrangements(ProductGroupTask streamTask)` in `ProductIngestionSaga` public

## [2.19.0]
### Changed
- Including user preferences in arrangements data

## [2.18.0]
### Changed
- Upgrade plugins `maven-surefire-plugin` and `maven-failsafe-plugin`

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

## [2.14.0]
### Fixed
- Fix ingestion of arrangements with child/parent relation by ordering and sequencing DBS requests

## [2.13.0]
### Added
- Add debit and credit indicator for CustomProduct

## [2.12.0]
### Added
- Enabling Sleuth trace ids to be propagated to the api response headers

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

## [2.8.0]
### Added
- Added BBAN to TermDeposit in LegalEntity spec

## [2.7.0]
### Fixed
- Fixed configuration for Loan Mapping

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

[2.64.0]: https://github.com/Backbase/stream-services/compare/2.62.0...2.64.0
[2.62.0]: https://github.com/Backbase/stream-services/compare/2.61.0...2.62.0
[2.61.0]: https://github.com/Backbase/stream-services/compare/2.52.0...2.61.0
[2.52.0]: https://github.com/Backbase/stream-services/compare/2.51.0...2.52.0
[2.51.0]: https://github.com/Backbase/stream-services/compare/2.48.0...2.51.0
[2.48.0]: https://github.com/Backbase/stream-services/compare/2.47.0...2.48.0
[2.47.0]: https://github.com/Backbase/stream-services/compare/2.46.3...2.47.0
[2.46.3]: https://github.com/Backbase/stream-services/compare/2.46.2...2.46.3
[2.46.2]: https://github.com/Backbase/stream-services/compare/2.46.1...2.46.2
[2.46.1]: https://github.com/Backbase/stream-services/compare/2.46.0...2.46.1
[2.46.0]: https://github.com/Backbase/stream-services/compare/2.45.0...2.46.0
[2.45.0]: https://github.com/Backbase/stream-services/compare/2.44.0...2.45.0
[2.44.0]: https://github.com/Backbase/stream-services/compare/2.43.0...2.44.0
[2.43.0]: https://github.com/Backbase/stream-services/compare/2.42.0...2.43.0
[2.42.0]: https://github.com/Backbase/stream-services/compare/2.41.0...2.42.0
[2.41.0]: https://github.com/Backbase/stream-services/compare/2.40.0...2.41.0
[2.40.0]: https://github.com/Backbase/stream-services/compare/2.39.0...2.40.0
[2.39.0]: https://github.com/Backbase/stream-services/compare/2.38.0...2.39.0
[2.38.0]: https://github.com/Backbase/stream-services/compare/2.37.0...2.38.0
[2.37.0]: https://github.com/Backbase/stream-services/compare/2.36.0...2.37.0
[2.36.0]: https://github.com/Backbase/stream-services/compare/2.35.0...2.36.0
[2.35.0]: https://github.com/Backbase/stream-services/compare/2.34.0...2.35.0
[2.34.0]: https://github.com/Backbase/stream-services/compare/2.33.0...2.34.0
[2.33.0]: https://github.com/Backbase/stream-services/compare/2.32.0...2.33.0
[2.32.0]: https://github.com/Backbase/stream-services/compare/2.31.0...2.32.0
[2.31.0]: https://github.com/Backbase/stream-services/compare/2.30.0...2.31.0
[2.30.0]: https://github.com/Backbase/stream-services/compare/2.29.0...2.30.0
[2.29.0]: https://github.com/Backbase/stream-services/compare/2.28.0...2.29.0
[2.28.0]: https://github.com/Backbase/stream-services/compare/2.27.0...2.28.0
[2.27.0]: https://github.com/Backbase/stream-services/compare/2.26.0...2.27.0
[2.26.0]: https://github.com/Backbase/stream-services/compare/2.25.0...2.26.0
[2.25.0]: https://github.com/Backbase/stream-services/compare/2.24.0...2.25.0
[2.24.0]: https://github.com/Backbase/stream-services/compare/2.23.0...2.24.0
[2.23.0]: https://github.com/Backbase/stream-services/compare/2.22.0...2.23.0
[2.22.0]: https://github.com/Backbase/stream-services/compare/2.21.0...2.22.0
[2.21.0]: https://github.com/Backbase/stream-services/compare/2.20.0...2.21.0
[2.20.0]: https://github.com/Backbase/stream-services/compare/2.19.0...2.20.0
[2.19.0]: https://github.com/Backbase/stream-services/compare/2.18.0...2.19.0
[2.18.0]: https://github.com/Backbase/stream-services/compare/2.17.0...2.18.0
[2.17.0]: https://github.com/Backbase/stream-services/compare/2.16.0...2.17.0
[2.16.0]: https://github.com/Backbase/stream-services/compare/2.15.0...2.16.0
[2.15.0]: https://github.com/Backbase/stream-services/compare/2.14.0...2.15.0
[2.14.0]: https://github.com/Backbase/stream-services/compare/2.13.0...2.14.0
[2.13.0]: https://github.com/Backbase/stream-services/compare/2.12.0...2.13.0
[2.12.0]: https://github.com/Backbase/stream-services/compare/2.11.0...2.12.0
[2.11.0]: https://github.com/Backbase/stream-services/compare/2.10.0...2.11.0
[2.10.0]: https://github.com/Backbase/stream-services/compare/2.9.0...2.10.0
[2.9.0]: https://github.com/Backbase/stream-services/compare/2.8.0...2.9.0
[2.8.0]: https://github.com/Backbase/stream-services/compare/2.7.0...2.8.0
[2.7.0]: https://github.com/Backbase/stream-services/compare/2.6.0...2.7.0
[2.6.0]: https://github.com/Backbase/stream-services/releases/tag/2.6.0
[2.70.1]: https://github.com/Backbase/stream-services/compare/2.71.0...2.70.1
[2.75.0]: https://github.com/Backbase/stream-services/compare/2.74.0...2.75.0
